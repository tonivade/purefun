/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Consumer3;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Try;

/**
 * <p>This type is an abstraction of a computation deferred in time, that will be eventually executed in the future to obtain
 * a result. Despite other implementations of Future, this implementation doesn't execute immediately the computation in another
 * thread. Only when the method {@code apply} is called, providing an executor or using the default executor, the computation is started.
 * Then, the result of calling {@code apply} method, is a {@code Promise}. The user of the library can subscribe to events of the promise like
 * {@code onComplete}, {@code onSuccess} or {@code onFailure}; or call to {@code get} method and wait to obtain the result of the computation.</p>
 *
 * <p>The result of the computation is a {@code Try<T>}, this means that the computation can end successfully or with an error.</p>
 *
 * <p>You can create instances of {@code Future} in this way:</p>
 * <ul>
 *   <li>Future.success(value): returns a future that returns successfully with the given value.</li>
 *   <li>Future.failure(error): returns a future that returns an error with the given error.</li>
 *   <li>Future.async(computation): returns a future that eventually will execute the given computation.</li>
 *   <li>Future.exec(runnable): returns a future that eventually will execute the given runnable.</li>
 *   <li>Future.delay(duration, computation): returns a future that eventually will execute the given computation, but after waiting the given duration.</li>
 *   <li>Future.defer(computation): returns a future that eventually will execute the given computation that returns another Future.</li>
 *   <li>Future.bracket(acquire, usage, release): returns a future that eventually will acquire a resource, then use it, and finally release it.</li>
 * </ul>
 *
 * <p>A future can be cancelable by calling the method {@code cancel}. If the future has not been executed yet, the future will be cancelled
 * and the result of the computation will be a {@code Try.failure(CancellableException)}, but if the future has been executed, and is completed
 * the calling of cancel method will not have any consequences. If the computation is running when the cancel method is called, and if the flag
 * mayInterruptThread is true, then it will try to interrupt the thread running the computation and the result of the computation
 * will be also a {@code Try.failure(CancellableException)}.</p>
 *
 * <p>If during the execution of the computation in a thread, this thread is interrupted for any reason, the result
 * of the computation will be a {@code Try.failure(InterruptedException)}.</p>
 *
 * @param <T> result of the computation
 * @see Try
 * @see Promise
 */
@HigherKind
public interface Future<T> {

  Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

  Promise<T> apply(Executor executor);

  default Promise<T> apply() {
    return apply(DEFAULT_EXECUTOR);
  }

  void cancel(boolean mayInterruptThread);

  default Try<T> await() {
    return await(DEFAULT_EXECUTOR);
  }

  default Try<T> await(Executor executor) {
    return apply(executor).get();
  }

  <R> Future<R> map(Function1<T, R> mapper);

  <R> Future<R> flatMap(Function1<T, Future<R>> mapper);

  <R> Future<R> andThen(Future<R> next);

  Future<T> filter(Matcher1<T> matcher);

  default Future<T> filterNot(Matcher1<T> matcher) {
    return filter(matcher.negate());
  }

  Future<T> orElse(Future<T> other);

  Future<T> recover(Function1<Throwable, T> mapper);

  <X extends Throwable> Future<T> recoverWith(Class<X> type, Function1<X, T> mapper);

  <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper);

  FutureModule getModule();

  static <T> Future<T> success(T value) {
    return FutureImpl.sync(() -> Try.success(value));
  }

  static <T> Future<T> failure(Throwable error) {
    return FutureImpl.sync(() -> Try.failure(error));
  }

  static <T> Future<T> from(Callable<T> callable) {
    return async(callable::call);
  }

  static <T> Future<T> from(java.util.concurrent.Future<T> future) {
    return async(future::get);
  }

  static <T> Future<T> async(Producer<T> task) {
    return FutureImpl.async(task.liftTry());
  }

  static Future<Unit> exec(CheckedRunnable task) {
    return async(() -> { task.run(); return Unit.unit(); });
  }

  static <T> Future<T> delay(Duration timeout, Producer<T> producer) {
    return async(() -> { MILLISECONDS.sleep(timeout.toMillis()); return producer.get(); });
  }

  static <T> Future<T> defer(Producer<Future<T>> producer) {
    return async(producer::get).flatMap(identity());
  }

  static <T extends AutoCloseable, R> Future<R> bracket(Future<T> acquire, Function1<T, Future<R>> use) {
    return FutureImpl.bracket(acquire, use, AutoCloseable::close);
  }

  static <T, R> Future<R> bracket(Future<T> acquire, Function1<T, Future<R>> use, Consumer1<T> release) {
    return FutureImpl.bracket(acquire, use, release);
  }
}

interface FutureModule { }

final class FutureImpl<T> implements Future<T> {

  private final Promise<T> promise = Promise.make();
  private final Cancellable<T> cancellable = Cancellable.from(promise);
  private final Consumer3<Executor, Promise<T>, Cancellable<T>> callback;

  private FutureImpl(Consumer3<Executor, Promise<T>, Cancellable<T>> callback) {
    this.callback = requireNonNull(callback);
  }

  @Override
  public Promise<T> apply(Executor executor) {
    callback.accept(executor, promise, cancellable);
    return promise;
  }

  @Override
  public <R> Future<R> map(Function1<T, R> mapper) {
    return transform(this, value -> value.map(mapper));
  }

  @Override
  public <R> Future<R> flatMap(Function1<T, Future<R>> mapper) {
    return chain(this,
        value -> value.fold(Future::failure, mapper));
  }

  @Override
  public <R> Future<R> andThen(Future<R> next) {
    return flatMap(ignore -> next);
  }

  @Override
  public Future<T> filter(Matcher1<T> matcher) {
    return transform(this, value -> value.filter(matcher));
  }

  @Override
  public Future<T> orElse(Future<T> other) {
    return chain(this, value -> value.fold(cons(other), Future::success));
  }

  @Override
  public Future<T> recover(Function1<Throwable, T> mapper) {
    return transform(this, value -> value.recover(mapper));
  }

  @Override
  public <X extends Throwable> Future<T> recoverWith(Class<X> type, Function1<X, T> mapper) {
    return transform(this, value -> value.recoverWith(type, mapper));
  }

  @Override
  public <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper) {
    return transform(this, value -> Try.success(value.fold(failureMapper, successMapper)));
  }

  @Override
  public void cancel(boolean mayInterruptThread) {
    cancellable.cancel(mayInterruptThread);
  }

  @Override
  public FutureModule getModule() {
    throw new UnsupportedOperationException();
  }

  protected static <T> Future<T> sync(Producer<Try<T>> producer) {
    return new FutureImpl<>((executor, promise, cancel) -> promise.tryComplete(producer.get()));
  }

  protected static <T, R> Future<R> transform(Future<T> current, Function1<Try<T>, Try<R>> mapper) {
    return new FutureImpl<>(
        (executor, promise, cancellable) ->
          current.apply(executor).onComplete(value -> promise.tryComplete(mapper.apply(value))));
  }

  protected static <T, R> Future<R> chain(Future<T> current, Function1<Try<T>, Future<R>> mapper) {
    return new FutureImpl<>(
        (executor, promise, cancellable) ->
          current.apply(executor).onComplete(
            value -> mapper.apply(value).apply(executor).onComplete(promise::tryComplete)));
  }

  protected static <T> Future<T> async(Producer<Try<T>> producer) {
    return new FutureImpl<>(
        (executor, promise, cancellable) ->
          executor.execute(() -> {
            cancellable.updateThread();
            promise.tryComplete(producer.get());
          }));
  }

  protected static <T> Future<T> from(Promise<T> promise) {
    return new FutureImpl<>((executor, current, cancel) -> promise.onComplete(current::tryComplete));
  }

  protected static <T, R> Future<R> bracket(Future<T> acquire, Function1<T, Future<R>> use, Consumer1<T> release) {
    return new FutureImpl<>(
      (executor, promise, cancellable) ->
        acquire.apply(executor).onComplete(
          resource -> resource.fold(Future::failure, use).apply(executor)
            .onComplete(promise::tryComplete)
            .onComplete(result -> resource.onSuccess(release))));
  }
}