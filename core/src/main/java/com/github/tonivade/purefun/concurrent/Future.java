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
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;

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

  Future<T> onSuccess(Consumer1<? super T> callback);
  Future<T> onFailure(Consumer1<? super Throwable> callback);
  Future<T> onComplete(Consumer1<? super Try<? extends T>> callback);

  <R> Future<R> map(Function1<? super T, ? extends R> mapper);

  <R> Future<R> flatMap(Function1<? super T, ? extends Future<? extends R>> mapper);

  <R> Future<R> andThen(Future<? extends R> next);

  <R> Future<R> ap(Future<Function1<? super T, ? extends R>> apply);

  Future<T> filter(Matcher1<? super T> matcher);

  default Future<T> filterNot(Matcher1<? super T> matcher) {
    return filter(matcher.negate());
  }

  Future<T> orElse(Future<T> other);

  default T get() {
    return getOrElseThrow();
  }

  default T getOrElse(T value) {
    return getOrElse(Producer.cons(value));
  }

  default T getOrElse(Producer<? extends T> value) {
    return await().getOrElse(value);
  }

  default T getOrElseThrow() {
    return await().getOrElseThrow();
  }

  default <X extends Throwable> T getOrElseThrow(Producer<? extends X> producer) throws X {
    return await().getOrElseThrow(producer);
  }

  default Throwable getCause() {
    return await().getCause();
  }

  default Future<T> recover(Function1<? super Throwable, ? extends T> mapper) {
    return fold(mapper, identity());
  }

  <X extends Throwable> Future<T> recoverWith(Class<X> type, Function1<? super X, ? extends T> mapper);

  <U> Future<U> fold(
      Function1<? super Throwable, ? extends U> failureMapper, 
      Function1<? super T, ? extends U> successMapper);

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

  static <T> Future<T> from(Callable<? extends T> callable) {
    return from(DEFAULT_EXECUTOR, callable);
  }

  static <T> Future<T> from(Executor executor, Callable<? extends T> callable) {
    return task(executor, callable::call);
  }

  static <T> Future<T> from(java.util.concurrent.Future<? extends T> future) {
    return from(DEFAULT_EXECUTOR, future);
  }

  static <T> Future<T> from(Executor executor, java.util.concurrent.Future<? extends T> future) {
    return task(executor, future::get);
  }

  static <T> Future<T> task(Producer<? extends T> task) {
    return task(DEFAULT_EXECUTOR, task);
  }

  static <T> Future<T> task(Executor executor, Producer<? extends T> task) {
    return FutureImpl.task(executor, task.liftTry());
  }

  static Future<Unit> exec(CheckedRunnable task) {
    return exec(DEFAULT_EXECUTOR, task);
  }

  static Future<Unit> exec(Executor executor, CheckedRunnable task) {
    return task(executor, () -> { task.run(); return unit(); });
  }

  static <T> Future<T> delay(Duration timeout, Producer<? extends T> producer) {
    return delay(DEFAULT_EXECUTOR, timeout, producer);
  }

  static <T> Future<T> delay(Executor executor, Duration timeout, Producer<? extends T> producer) {
    return sleep(executor, timeout).flatMap(x -> task(executor, producer));
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

  // TODO:
  static <T> Future<T> defer(Executor executor, Producer<Future<T>> producer) {
    return task(executor, producer::get).flatMap(identity());
  }

  static <T extends AutoCloseable, R> Future<R> bracket(Future<T> acquire, Function1<T, Future<R>> use) {
    return bracket(DEFAULT_EXECUTOR, acquire, use);
  }

  static <T extends AutoCloseable, R> Future<R> bracket(Executor executor, 
      Future<? extends T> acquire, 
      Function1<? super T, ? extends Future<? extends R>> use) {
    return FutureImpl.bracket(executor, acquire, use, AutoCloseable::close);
  }

  static <T, R> Future<R> bracket(Future<? extends T> acquire, 
      Function1<? super T, ? extends Future<? extends R>> use, 
      Consumer1<? super T> release) {
    return bracket(DEFAULT_EXECUTOR, acquire, use, release);
  }

  static <T, R> Future<R> bracket(Executor executor, 
      Future<? extends T> acquire, 
      Function1<? super T, ? extends Future<? extends R>> use, 
      Consumer1<? super T> release) {
    return FutureImpl.bracket(executor, acquire, use, release);
  }

  // TODO
  static <A> Future<Sequence<A>> traverse(Sequence<Future<A>> sequence) {
    return sequence.foldLeft(success(ImmutableList.empty()), 
        (Future<Sequence<A>> xs, Future<A> a) -> map2(xs, a, Sequence::append));
  }
  
  static <T, V, R> Future<R> map2(Future<T> fa, Future<V> fb, Function2<? super T, ? super V, ? extends R> mapper) {
    return fb.ap(fa.map(mapper.curried()));
  }
  
  static <T, V> Future<Tuple2<T, V>> tuple(Future<T> fa, Future<V> fb) {
    return map2(fa, fb, Tuple2::of);
  }

  // TODO
  static <T> Future<T> async(Consumer1<Consumer1<Try<T>>> consumer) {
    return async(DEFAULT_EXECUTOR, consumer);
  }

  // TODO
  static <T> Future<T> async(Executor executor, Consumer1<Consumer1<Try<T>>> consumer) {
    return FutureImpl.async(executor, consumer);
  }
}

final class FutureImpl<T> implements SealedFuture<T> {

  private final Executor executor;
  private final Propagate propagate;
  private final Promise<T> promise;
  private final Cancellable<T> cancellable;

  private FutureImpl(Executor executor, Callback<T> callback) {
    this(executor, callback, Propagate.noPropagate());
  }

  private FutureImpl(Executor executor, Callback<T> callback, Propagate propagate) {
    this.executor = checkNonNull(executor);
    this.propagate = checkNonNull(propagate);
    this.promise = Promise.make(executor);
    this.cancellable = Cancellable.from(promise);
    callback.accept(promise, cancellable);
  }

  @Override
  public Future<T> onComplete(Consumer1<? super Try<? extends T>> consumer) {
    promise.onComplete(consumer);
    return this;
  }

  @Override
  public Future<T> onSuccess(Consumer1<? super T> consumer) {
    promise.onSuccess(consumer);
    return this;
  }

  @Override
  public Future<T> onFailure(Consumer1<? super Throwable> consumer) {
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
  public <R> Future<R> map(Function1<? super T, ? extends R> mapper) {
    return transform(value -> value.map(mapper));
  }

  @Override
  public <R> Future<R> flatMap(Function1<? super T, ? extends Future<? extends R>> mapper) {
    return chain(value -> value.fold(e -> Future.failure(executor, e), mapper));
  }

  @Override
  public <R> Future<R> andThen(Future<? extends R> next) {
    return flatMap(ignore -> next);
  }
  
  @Override
  public <R> Future<R> ap(Future<Function1<? super T, ? extends R>> apply) {
    checkNonNull(apply);
    return new FutureImpl<>(executor, 
        (p, c) -> promise.onComplete(try1 -> apply.onComplete(
            try2 -> p.tryComplete(Try.map2(try1, try2, (t, f) -> f.apply(t))))), this::cancel);
  }

  @Override
  public Future<T> filter(Matcher1<? super T> matcher) {
    return transform(value -> value.filter(matcher));
  }

  @Override
  public Future<T> orElse(Future<T> other) {
    return chain(value -> value.fold(cons(other), t -> Future.success(executor, t)));
  }

  @Override
  public <X extends Throwable> Future<T> recoverWith(Class<X> type, Function1<? super X, ? extends T> mapper) {
    return transform(value -> {
      Try<T> try1 = TryOf.narrowK(value);
      return try1.recoverWith(type, mapper);
    });
  }

  @Override
  public <U> Future<U> fold(Function1<? super Throwable, ? extends U> failureMapper, Function1<? super T, ? extends U> successMapper) {
    return transform(value -> Try.success(value.fold(failureMapper, successMapper)));
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

  private <R> Future<R> transform(Function1<? super Try<? extends T>, ? extends Try<? extends R>> mapper) {
    checkNonNull(mapper);
    return new FutureImpl<>(executor,
        (p, c) ->
          promise.onComplete(value -> p.tryComplete(mapper.apply(value))), this::cancel);
  }

  private <R> Future<R> chain(Function1<? super Try<? extends T>, ? extends Future<? extends R>> mapper) {
    checkNonNull(executor);
    checkNonNull(mapper);
    return new FutureImpl<>(executor,
        (p, c) ->
          promise.onComplete(value -> mapper.apply(value).onComplete(p::tryComplete)), this::cancel);
  }

  protected static <T> Future<T> sync(Executor executor, Try<? extends T> result) {
    checkNonNull(executor);
    checkNonNull(result);
    return new FutureImpl<>(executor, (p, c) -> p.tryComplete(result));
  }

  protected static <T> Future<T> task(Executor executor, Producer<? extends Try<? extends T>> producer) {
    checkNonNull(executor);
    checkNonNull(producer);
    return new FutureImpl<>(executor,
        (p, c) -> executor.execute(() -> {
            c.updateThread();
            p.tryComplete(producer.get());
          }));
  }

  // TODO
  protected static <T> Future<T> async(Executor executor, Consumer1<Consumer1<Try<T>>> consumer) {
    checkNonNull(executor);
    checkNonNull(consumer);
    return new FutureImpl<>(executor, 
        (p, c) -> executor.execute(() -> {
            c.updateThread();
            consumer.accept(p::tryComplete);
          }));
  }

  protected static <T> Future<T> from(Executor executor, Promise<? extends T> promise) {
    checkNonNull(executor);
    checkNonNull(promise);
    return new FutureImpl<>(executor, (p, c) -> promise.onComplete(p::tryComplete));
  }

  protected static Future<Unit> sleep(Executor executor, Duration delay) {
    checkNonNull(executor);
    checkNonNull(delay);
    return from(executor, FutureModule.sleep(executor, delay));
  }

  protected static <T, R> Future<R> bracket(Executor executor, Future<? extends T> acquire, Function1<? super T, ? extends Future<? extends R>> use, Consumer1<? super T> release) {
    checkNonNull(executor);
    checkNonNull(acquire);
    checkNonNull(use);
    checkNonNull(release);
    return new FutureImpl<>(executor,
        (p, c) -> acquire.onComplete(
                resource -> resource.fold(e -> Future.<R>failure(executor, e), use)
                  .onComplete(p::tryComplete)
                  .onComplete(result -> resource.onSuccess(release))));
  }
}

interface FutureModule {

  ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(0);

  static Promise<Unit> sleep(Executor executor, Duration delay) {
    return Promise.from(executor, supplyAsync(Unit::unit, delayedExecutor(delay, executor)));
  }

  static Executor delayedExecutor(Duration delay, Executor executor) {
    return task -> SCHEDULER.schedule(() -> executor.execute(task), delay.toMillis(), TimeUnit.MILLISECONDS);
  }
}

interface Callback<T> {
  
  void accept(Promise<T> promise, Cancellable<T> cancellable);
}

interface Propagate {

  void accept(boolean value);

  static Propagate noPropagate() {
    return value -> {};
  }
}