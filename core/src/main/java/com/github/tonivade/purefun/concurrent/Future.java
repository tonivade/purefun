/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Unit.unit;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Consumer2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Try;

/**
 * <p>This type is an abstraction of a computation executed in another thread. To run the computation an {@code Executor}
 * should be provided. If no {@code Executor} is provided, then a default instance is used.</p>
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
@HigherKind(sealed = true)
public interface Future<T> extends FutureOf<T> {

  Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

  Try<T> await();
  Try<T> await(Duration timeout);

  void cancel(boolean mayInterruptThread);

  boolean isCompleted();
  boolean isCancelled();

  Future<T> onSuccess(Consumer1<T> callback);
  Future<T> onFailure(Consumer1<Throwable> callback);
  Future<T> onComplete(Consumer1<Try<T>> callback);

  <R> Future<R> map(Function1<T, R> mapper);

  <R> Future<R> flatMap(Function1<T, Future<R>> mapper);

  <R> Future<R> andThen(Future<R> next);

  Future<T> filter(Matcher1<T> matcher);

  default Future<T> filterNot(Matcher1<T> matcher) {
    return filter(matcher.negate());
  }

  Future<T> orElse(Future<T> other);

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

  default Future<T> recover(Function1<Throwable, T> mapper) {
    return fold(mapper, identity());
  }

  <X extends Throwable> Future<T> recoverWith(Class<X> type, Function1<X, T> mapper);

  <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper);

  default CompletableFuture<T> toCompletableFuture() {
    CompletableFuture<T> completableFuture = new CompletableFuture<>();
    onSuccess(completableFuture::complete);
    onFailure(completableFuture::completeExceptionally);
    return completableFuture;
  }

  Promise<T> toPromise();

  static <T> Future<T> success(T value) {
    return success(DEFAULT_EXECUTOR, value);
  }

  static <T> Future<T> success(Executor executor, T value) {
    return FutureImpl.sync(executor, Try.success(value));
  }

  static <T> Future<T> failure(Throwable error) {
    return failure(DEFAULT_EXECUTOR, error);
  }

  static <T> Future<T> failure(Executor executor, Throwable error) {
    return FutureImpl.sync(executor, Try.failure(error));
  }

  static <T> Future<T> from(Callable<T> callable) {
    return from(DEFAULT_EXECUTOR, callable);
  }

  static <T> Future<T> from(Executor executor, Callable<T> callable) {
    return async(executor, callable::call);
  }

  static <T> Future<T> from(java.util.concurrent.Future<T> future) {
    return from(DEFAULT_EXECUTOR, future);
  }

  static <T> Future<T> from(Executor executor, java.util.concurrent.Future<T> future) {
    return async(executor, future::get);
  }

  static <T> Future<T> async(Producer<T> task) {
    return async(DEFAULT_EXECUTOR, task);
  }

  static <T> Future<T> async(Executor executor, Producer<T> task) {
    return FutureImpl.async(executor, task.liftTry());
  }

  static Future<Unit> exec(CheckedRunnable task) {
    return exec(DEFAULT_EXECUTOR, task);
  }

  static Future<Unit> exec(Executor executor, CheckedRunnable task) {
    return async(executor, () -> { task.run(); return unit(); });
  }

  static <T> Future<T> delay(Duration timeout, Producer<T> producer) {
    return delay(DEFAULT_EXECUTOR, timeout, producer);
  }

  static <T> Future<T> delay(Executor executor, Duration timeout, Producer<T> producer) {
    return sleep(executor, timeout).flatMap(x -> async(executor, producer));
  }

  static Future<Unit> sleep(Duration delay) {
    return sleep(DEFAULT_EXECUTOR, delay);
  }

  static Future<Unit> sleep(Executor executor, Duration delay) {
    return FutureImpl.sleep(executor, delay);
  }

  static <T> Future<T> defer(Producer<Future<T>> producer) {
    return defer(DEFAULT_EXECUTOR, producer);
  }

  static <T> Future<T> defer(Executor executor, Producer<Future<T>> producer) {
    return async(executor, producer::get).flatMap(identity());
  }

  static <T extends AutoCloseable, R> Future<R> bracket(Future<T> acquire, Function1<T, Future<R>> use) {
    return bracket(DEFAULT_EXECUTOR, acquire, use);
  }

  static <T extends AutoCloseable, R> Future<R> bracket(Executor executor, Future<T> acquire, Function1<T, Future<R>> use) {
    return FutureImpl.bracket(executor, acquire, use, AutoCloseable::close);
  }

  static <T, R> Future<R> bracket(Future<T> acquire, Function1<T, Future<R>> use, Consumer1<T> release) {
    return bracket(DEFAULT_EXECUTOR, acquire, use, release);
  }

  static <T, R> Future<R> bracket(Executor executor, Future<T> acquire, Function1<T, Future<R>> use, Consumer1<T> release) {
    return FutureImpl.bracket(executor, acquire, use, release);
  }
}

final class FutureImpl<T> implements SealedFuture<T> {

  private final Executor executor;
  private final Consumer1<Boolean> propagate;
  private final Promise<T> promise;
  private final Cancellable<T> cancellable;

  private FutureImpl(Executor executor, Consumer2<Promise<T>, Cancellable<T>> callback) {
    this(executor, callback, x -> {});
  }

  protected FutureImpl(Executor executor, Consumer2<Promise<T>, Cancellable<T>> callback, Consumer1<Boolean> propagate) {
    this.executor = checkNonNull(executor);
    this.propagate = checkNonNull(propagate);
    this.promise = Promise.make(executor);
    this.cancellable = Cancellable.from(promise);
    callback.accept(promise, cancellable);
  }

  @Override
  public Future<T> onComplete(Consumer1<Try<T>> consumer) {
    promise.onComplete(consumer);
    return this;
  }

  @Override
  public Future<T> onSuccess(Consumer1<T> consumer) {
    promise.onSuccess(consumer);
    return this;
  }

  @Override
  public Future<T> onFailure(Consumer1<Throwable> consumer) {
    promise.onFailure(consumer);
    return this;
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
  public Try<T> await() {
    return promise.get();
  }

  @Override
  public Try<T> await(Duration timeout) {
    return promise.get(timeout);
  }

  @Override
  public <R> Future<R> map(Function1<T, R> mapper) {
    return transform(executor, this, value -> value.map(mapper));
  }

  @Override
  public <R> Future<R> flatMap(Function1<T, Future<R>> mapper) {
    return chain(executor, this, value -> value.fold(e -> Future.failure(executor, e), mapper));
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
    return chain(executor, this, value -> value.fold(cons(other), t -> Future.success(executor, t)));
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
  public void cancel(boolean mayInterruptThread) {
    try {
      cancellable.cancel(mayInterruptThread);
    } finally {
      propagate.accept(mayInterruptThread);
    }
  }

  @Override
  public Promise<T> toPromise() {
    return promise;
  }

  protected static <T> Future<T> sync(Executor executor, Try<T> result) {
    checkNonNull(executor);
    checkNonNull(result);
    return new FutureImpl<>(executor, (promise, cancel) -> promise.tryComplete(result));
  }

  protected static <T, R> Future<R> transform(Executor executor, Future<T> current, Function1<Try<T>, Try<R>> mapper) {
    checkNonNull(executor);
    checkNonNull(current);
    checkNonNull(mapper);
    return new FutureImpl<>(executor,
        (promise, cancellable) ->
          current.onComplete(value -> promise.tryComplete(mapper.apply(value))), current::cancel);
  }

  protected static <T, R> Future<R> chain(Executor executor, Future<T> current, Function1<Try<T>, Future<R>> mapper) {
    checkNonNull(executor);
    checkNonNull(current);
    checkNonNull(mapper);
    return new FutureImpl<>(executor,
        (promise, cancellable) ->
          current.onComplete(value -> mapper.apply(value).onComplete(promise::tryComplete)), current::cancel);
  }

  protected static <T> Future<T> async(Executor executor, Producer<Try<T>> producer) {
    checkNonNull(executor);
    checkNonNull(producer);
    return new FutureImpl<>(executor,
        (promise, cancellable) ->
          executor.execute(() -> {
            cancellable.updateThread();
            promise.tryComplete(producer.get());
          }));
  }

  protected static <T> Future<T> from(Executor executor, Promise<T> promise) {
    checkNonNull(executor);
    checkNonNull(promise);
    return new FutureImpl<>(executor, (current, cancel) -> promise.onComplete(current::tryComplete));
  }

  protected static Future<Unit> sleep(Executor executor, Duration delay) {
    checkNonNull(executor);
    checkNonNull(delay);
    return from(executor, Delayed.sleep(executor, delay));
  }

  protected static <T, R> Future<R> bracket(Executor executor, Future<T> acquire, Function1<T, Future<R>> use, Consumer1<T> release) {
    checkNonNull(executor);
    checkNonNull(acquire);
    checkNonNull(use);
    checkNonNull(release);
    return new FutureImpl<>(executor,
        (promise, cancellable) ->
            acquire.onComplete(
                resource -> resource.fold(e -> Future.failure(executor, e), use)
                  .onComplete(promise::tryComplete)
                  .onComplete(result -> resource.onSuccess(release))));
  }
}

final class Delayed {

  private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(0);

  protected static Promise<Unit> sleep(Executor executor, Duration delay) {
    return Promise.from(executor, supplyAsync(Unit::unit, delayedExecutor(delay, executor)));
  }

  private static Executor delayedExecutor(Duration delay, Executor executor) {
    return task -> SCHEDULER.schedule(() -> executor.execute(task), delay.toMillis(), TimeUnit.MILLISECONDS);
  }
}