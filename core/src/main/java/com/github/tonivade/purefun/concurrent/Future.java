/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.Function1.cons;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.github.tonivade.purefun.CheckedProducer;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Consumer2;
import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Try;

@HigherKind
public interface Future<T> extends FlatMap1<Future.µ, T>, Holder<T>, Filterable<T> {

  Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

  Try<T> await();
  Try<T> await(Duration timeout);

  void cancel(boolean mayInterruptThread);

  boolean isCompleted();
  boolean isCancelled();

  default boolean isSuccess() {
    return await().isSuccess();
  }

  default boolean isFailure() {
    return await().isFailure();
  }

  Future<T> onSuccess(Consumer1<T> callback);
  Future<T> onFailure(Consumer1<Throwable> callback);
  Future<T> onComplete(Consumer1<Try<T>> callback);

  @Override
  <R> Future<R> map(Function1<T, R> mapper);

  @Override
  <R> Future<R> flatMap(Function1<T, ? extends Higher1<Future.µ, R>> mapper);

  <R> Future<R> andThen(Future<R> next);

  @Override
  Future<T> filter(Matcher1<T> matcher);

  Future<T> orElse(Future<T> other);

  @Override
  default T get() {
    return getOrElseThrow(NoSuchElementException::new);
  }

  default T getOrElse(T value) {
    return getOrElse(Producer.cons(value));
  }

  default T getOrElse(Producer<T> value) {
    return await().getOrElse(value);
  }

  default <X extends Throwable> T getOrElseThrow(Producer<X> producer) throws X {
    return await().getOrElseThrow(producer);
  }

  default Throwable getCause() {
    return await().getCause();
  }

  @Override
  @SuppressWarnings("unchecked")
  default <V> Future<V> flatten() {
    return flatMap(value -> {
      try {
        return (Future<V>) value;
      } catch (ClassCastException e) {
        return Future.failure(new UnsupportedOperationException(e));
      }
    });
  }

  Future<T> recover(Function1<Throwable, T> mapper);

  <X extends Throwable> Future<T> recoverWith(Class<X> type, Function1<X, T> mapper);

  <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper);

  default CompletableFuture<T> toCompletableFuture() {
    CompletableFuture<T> completableFuture = new CompletableFuture<>();
    onSuccess(completableFuture::complete);
    onFailure(completableFuture::completeExceptionally);
    return completableFuture;
  }

  Promise<T> toPromise();

  FutureModule getModule();

  static <T> Future<T> success(T value) {
    return success(DEFAULT_EXECUTOR, value);
  }

  static <T> Future<T> success(Executor executor, T value) {
    return FutureImpl.sync(executor, () -> Try.success(value));
  }

  static <T> Future<T> failure(Throwable error) {
    return failure(DEFAULT_EXECUTOR, error);
  }

  static <T> Future<T> failure(Executor executor, Throwable error) {
    return FutureImpl.sync(executor, () -> Try.failure(error));
  }

  static <T> Future<T> from(Callable<T> callable) {
    return run(callable::call);
  }

  static <T> Future<T> from(java.util.concurrent.Future<T> future) {
    return run(future::get);
  }

  static <T> Future<T> run(CheckedProducer<T> task) {
    return run(DEFAULT_EXECUTOR, task);
  }

  static <T> Future<T> run(Executor executor, CheckedProducer<T> task) {
    return FutureImpl.async(executor, task.liftTry());
  }

  static Future<Unit> exec(CheckedRunnable task) {
    return exec(DEFAULT_EXECUTOR, task);
  }

  static Future<Unit> exec(Executor executor, CheckedRunnable task) {
    return run(executor, () -> { task.run(); return Unit.unit(); });
  }

  static <T> Future<T> delay(Duration timeout, CheckedProducer<T> producer) {
    return delay(DEFAULT_EXECUTOR, timeout, producer);
  }

  static <T> Future<T> delay(Executor executor, Duration timeout, CheckedProducer<T> producer) {
    return run(executor, () -> { MILLISECONDS.sleep(timeout.toMillis()); return producer.get(); });
  }

  static <T> Future<T> defer(CheckedProducer<Future<T>> producer) {
    return defer(DEFAULT_EXECUTOR, producer);
  }

  static <T> Future<T> defer(Executor executor, CheckedProducer<Future<T>> producer) {
    return run(executor, () -> producer.get()).flatten();
  }
}

interface FutureModule { }

final class FutureImpl<T> implements Future<T> {

  private final Executor executor;
  private final Promise<T> promise = Promise.make();
  private final Cancellable<T> cancellable = Cancellable.from(promise);

  private FutureImpl(Executor executor, Consumer2<Promise<T>, Cancellable<T>> task) {
    this.executor = requireNonNull(executor);
    requireNonNull(task).accept(promise, cancellable);
  }

  @Override
  public <R> Future<R> map(Function1<T, R> mapper) {
    return transform(executor, this, value -> value.map(mapper));
  }

  @Override
  public <R> Future<R> flatMap(Function1<T, ? extends Higher1<Future.µ, R>> mapper) {
    return chain(executor, this,
        value -> value.fold(Future::failure, mapper.andThen(Future::narrowK)));
  }

  @Override
  public <R> Future<R> andThen(Future<R> next) {
    return flatMap(ignore -> next);
  }

  @Override
  public Future<T> filter(Matcher1<T> matcher) {
    return transform(executor, this, value -> value.filter(matcher));
  }

  @Override
  public Future<T> orElse(Future<T> other) {
    return chain(executor, this, value -> value.fold(cons(other), Future::success));
  }

  @Override
  public Future<T> recover(Function1<Throwable, T> mapper) {
    return transform(executor, this, value -> value.recover(mapper));
  }

  @Override
  public <X extends Throwable> Future<T> recoverWith(Class<X> type, Function1<X, T> mapper) {
    return transform(executor, this, value -> value.recoverWith(type, mapper));
  }

  @Override
  public <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper) {
    return transform(executor, this, value -> Try.success(value.fold(failureMapper, successMapper)));
  }

  @Override
  public Promise<T> toPromise() {
    return promise;
  }

  @Override
  public Try<T> await() {
    return promise.get();
  }

  @Override
  public Try<T> await(Duration timeout) {
    return promise.get(timeout);
  }

  @Override
  public void cancel(boolean mayInterruptThread) {
    cancellable.cancel(mayInterruptThread);
  }

  @Override
  public boolean isCompleted() {
    return promise.isCompleted();
  }

  @Override
  public boolean isCancelled() {
    return cancellable.isCancelled();
  }

  @Override
  public Future<T> onSuccess(Consumer1<T> callback) {
    promise.onComplete(value -> value.onSuccess(callback));
    return this;
  }

  @Override
  public Future<T> onFailure(Consumer1<Throwable> callback) {
    promise.onComplete(value -> value.onFailure(callback));
    return this;
  }

  @Override
  public Future<T> onComplete(Consumer1<Try<T>> callback) {
    promise.onComplete(callback);
    return this;
  }

  @Override
  public FutureModule getModule() {
    throw new UnsupportedOperationException();
  }

  static <T> Future<T> sync(Executor executor, Producer<Try<T>> producer) {
    return new FutureImpl<>(executor, (promise, cancel) -> promise.tryComplete(producer.get()));
  }

  static <T, R> Future<R> transform(Executor executor, Future<T> current, Function1<Try<T>, Try<R>> mapper) {
    return new FutureImpl<>(executor,
        (promise, cancel) -> current.onComplete(value -> promise.tryComplete(mapper.apply(value))));
  }

  static <T, R> Future<R> chain(Executor executor, Future<T> current, Function1<Try<T>, Future<R>> mapper) {
    return new FutureImpl<>(executor,
        (promise, cancel) -> current.onComplete(value -> mapper.apply(value).onComplete(promise::tryComplete)));
  }

  static <T> Future<T> async(Executor executor, Producer<Try<T>> producer) {
    return new FutureImpl<>(executor,
        (promise, cancel) -> executor.execute(() -> { cancel.updateThread(); promise.tryComplete(producer.get()); }));
  }

  static <T> Future<T> from(Executor executor, Promise<T> promise) {
    return new FutureImpl<>(executor, (current, cancel) -> promise.onComplete(current::tryComplete));
  }
}