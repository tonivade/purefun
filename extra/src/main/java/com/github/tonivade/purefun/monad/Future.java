/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.CheckedProducer;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public interface Future<T> extends FlatMap1<Future.µ, T>, Holder<T>, Filterable<T> {

  final class µ implements Kind {}

  Try<T> await();
  Try<T> await(Duration timeout);

  void cancel();

  boolean isCompleted();
  boolean isCanceled();

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

  @Override
  Future<T> filter(Matcher1<T> matcher);

  Future<T> orElse(Future<T> other);

  @Override
  default T get() {
    return await().getOrElseThrow(NoSuchElementException::new);
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

  <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper);

  FutureModule getModule();

  static <T> Future<T> success(T value) {
    return success(FutureModule.DEFAULT_EXECUTOR, value);
  }

  static <T> Future<T> success(ExecutorService executor, T value) {
    return runTry(executor, () -> Try.success(value));
  }

  static <T> Future<T> failure(Throwable error) {
    return failure(FutureModule.DEFAULT_EXECUTOR, error);
  }

  static <T> Future<T> failure(ExecutorService executor, Throwable error) {
    return runTry(executor, () -> Try.failure(error));
  }

  static <T> Future<T> from(Callable<T> callable) {
    return run(callable::call);
  }

  static <T> Future<T> from(java.util.concurrent.Future<T> future) {
    return run(future::get);
  }

  static <T> Future<T> run(CheckedProducer<T> task) {
    return run(FutureModule.DEFAULT_EXECUTOR, task);
  }

  static <T> Future<T> run(ExecutorService executor, CheckedProducer<T> task) {
    return runTry(executor, task.liftTry());
  }

  static <T> Future<T> delay(Duration timeout, CheckedProducer<T> producer) {
    return delay(FutureModule.DEFAULT_EXECUTOR, timeout, producer);
  }

  static <T> Future<T> delay(ExecutorService executor, Duration timeout, CheckedProducer<T> producer) {
    return run(executor, () -> { MILLISECONDS.sleep(timeout.toMillis()); return producer.get(); });
  }

  static <T> Future<T> runTry(Producer<Try<T>> task) {
    return runTry(FutureModule.DEFAULT_EXECUTOR, task);
  }

  static <T> Future<T> runTry(ExecutorService executor, Producer<Try<T>> task) {
    return new FutureImpl<>(requireNonNull(executor), requireNonNull(task));
  }

  static <T> Future<T> narrowK(Higher1<Future.µ, T> hkt) {
    return (Future<T>) hkt;
  }

  final class FutureImpl<T> implements Future<T> {

    private final ExecutorService executor;
    private final java.util.concurrent.Future<?> job;
    private final AsyncValue<Try<T>> value = new AsyncValue<>();

    private FutureImpl(ExecutorService executor, Producer<Try<T>> task) {
      this.executor = executor;
      this.job = executor.submit(() -> value.set(task.get()));
    }

    @Override
    public <R> Future<R> map(Function1<T, R> mapper) {
      return runTry(executor, () -> await().map(mapper));
    }

    @Override
    public <R> Future<R> flatMap(Function1<T, ? extends Higher1<Future.µ, R>> mapper) {
      return runTry(executor,
          () -> await().flatMap(t -> mapper.andThen(Future::narrowK).apply(t).await()));
    }

    @Override
    public Future<T> filter(Matcher1<T> matcher) {
      return runTry(executor, () -> await().filter(matcher));
    }

    @Override
    public Future<T> orElse(Future<T> other) {
      return runTry(executor, () -> {
        if (Future.super.isSuccess()) {
          return this.await();
        }
        return other.await();
      });
    }

    @Override
    public Future<T> recover(Function1<Throwable, T> mapper) {
      return runTry(executor, () -> await().recover(mapper));
    }

    @Override
    public <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper) {
      return run(executor, () -> await().fold(failureMapper, successMapper));
    }

    @Override
    public Try<T> await() {
      return result().recover(Try::failure).get();
    }

    @Override
    public Try<T> await(Duration timeout) {
      return result(timeout).recover(Try::failure).get();
    }

    @Override
    public void cancel() {
      if (job.cancel(true)) {
        value.set(Try.failure(new CancellationException()));
      }
    }

    @Override
    public boolean isCompleted() {
      return !value.isEmpty();
    }

    @Override
    public boolean isCanceled() {
      return job.isCancelled();
    }

    @Override
    public Future<T> onSuccess(Consumer1<T> callback) {
      executor.execute(() -> await().onSuccess(callback));
      return this;
    }

    @Override
    public Future<T> onFailure(Consumer1<Throwable> callback) {
      executor.execute(() -> await().onFailure(callback));
      return this;
    }

    @Override
    public Future<T> onComplete(Consumer1<Try<T>> callback) {
      executor.execute(() -> callback.accept(await()));
      return this;
    }

    @Override
    public FutureModule getModule() {
      throw new UnsupportedOperationException();
    }

    private CheckedProducer<Try<T>> result() {
      return () -> value.get().getOrElse(Try.failure(new NoSuchElementException()));
    }

    private CheckedProducer<Try<T>> result(Duration timeout) {
      return () -> value.get(timeout).getOrElse(Try.failure(new NoSuchElementException()));
    }
  }
}

interface FutureModule {
  ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();
}

final class AsyncValue<T> {

  private final AtomicReference<Option<T>> reference = new AtomicReference<>(Option.none());
  private final CountDownLatch latch = new CountDownLatch(1);

  void set(T value) {
    if (reference.compareAndSet(Option.none(), Option.some(value))) {
      latch.countDown();
    }
    else throw new IllegalStateException("already setted: " + reference.get());
  }

  Option<T> get() throws InterruptedException {
    latch.await();
    return reference.get();
  }

  Option<T> get(Duration timeout) throws InterruptedException, TimeoutException {
    await(timeout);
    return reference.get();
  }

  boolean isEmpty() {
    return reference.get().equals(Option.none());
  }

  private void await(Duration timeout) throws InterruptedException, TimeoutException {
    if (!latch.await(timeout.toMillis(), MILLISECONDS)) {
      throw new TimeoutException();
    }
  }
}