/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Applicable;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.CheckedRunnable;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Function3;
import com.github.tonivade.purefun.core.Function4;
import com.github.tonivade.purefun.core.Function5;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;

@HigherKind
public sealed interface Promise<T> extends PromiseOf<T>, Bindable<Promise<?>, T>, Applicable<Promise<?>, T> permits PromiseImpl {

  boolean tryComplete(Try<? extends T> value);

  default Promise<T> cancel() {
    return failed(new CancellationException());
  }

  default Promise<T> complete(Try<? extends T> value) {
    if (tryComplete(value)) {
      return this;
    }
    throw new IllegalStateException("promise already completed");
  }

  default Promise<T> succeeded(T value) {
    return complete(Try.success(value));
  }

  default Promise<T> failed(Throwable error) {
    return complete(Try.failure(error));
  }

  Promise<T> onComplete(Consumer1<? super Try<? extends T>> consumer);

  default Promise<T> onSuccess(Consumer1<? super T> consumer) {
    return onComplete(value -> value.onSuccess(consumer));
  }

  default Promise<T> onFailure(Consumer1<? super Throwable> consumer) {
    return onComplete(value -> value.onFailure(consumer));
  }

  @Override
  <R> Promise<R> map(Function1<? super T, ? extends R> mapper);

  @Override
  <R> Promise<R> ap(Kind<Promise<?>, ? extends Function1<? super T, ? extends R>> apply);

  @Override
  default <R> Promise<R> andThen(Kind<Promise<?>, ? extends R> next) {
    return PromiseOf.toPromise(Bindable.super.andThen(next));
  }

  @Override
  <R> Promise<R> flatMap(Function1<? super T, ? extends Kind<Promise<?>, ? extends R>> mapper);

  default Promise<Unit> then(Consumer1<? super T> next) {
    return map(next.asFunction());
  }

  default Promise<Unit> thenRun(CheckedRunnable next) {
    return map(next.asProducer().asFunction());
  }

  Try<T> await();
  Try<T> await(Duration timeout);

  boolean isCompleted();

  static <T> Promise<T> make() {
    return make(Future.DEFAULT_EXECUTOR);
  }

  static <T> Promise<T> make(Executor executor) {
    return new PromiseImpl<>(executor);
  }

  static <T> Promise<T> from(CompletableFuture<? extends T> future) {
    return from(Future.DEFAULT_EXECUTOR, future);
  }

  static <T> Promise<T> from(Executor executor, CompletableFuture<? extends T> future) {
    Promise<T> promise = make(executor);
    future.whenCompleteAsync(
        (value, error) -> promise.tryComplete(
            value != null ? Try.success(value) : Try.failure(error)), executor);
    return promise;
  }

  static <A, B, C> Promise<C> mapN(Promise<? extends A> fa, Promise<? extends B> fb,
      Function2<? super A, ? super B, ? extends C> mapper) {
    return fb.ap(fa.map(mapper.curried()));
  }

  static <A, B, C, D> Promise<D> mapN(
      Promise<? extends A> fa,
      Promise<? extends B> fb,
      Promise<? extends C> fc,
      Function3<? super A, ? super B, ? super C, ? extends D> mapper) {
    return fc.ap(mapN(fa, fb, (a, b) -> mapper.curried().apply(a).apply(b)));
  }

  static <A, B, C, D, E> Promise<E> mapN(
      Promise<? extends A> fa,
      Promise<? extends B> fb,
      Promise<? extends C> fc,
      Promise<? extends D> fd,
      Function4<? super A, ? super B, ? super C, ? super D, ? extends E> mapper) {
    return fd.ap(mapN(fa, fb, fc, (a, b, c) -> mapper.curried().apply(a).apply(b).apply(c)));
  }

  static <A, B, C, D, E, R> Promise<R> mapN(
      Promise<? extends A> fa,
      Promise<? extends B> fb,
      Promise<? extends C> fc,
      Promise<? extends D> fd,
      Promise<? extends E> fe,
      Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> mapper) {
    return fe.ap(mapN(fa, fb, fc, fd, (a, b, c, d) -> mapper.curried().apply(a).apply(b).apply(c).apply(d)));
  }
}

final class PromiseImpl<T> implements Promise<T> {

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();
  private final Queue<Consumer1<? super Try<? extends T>>> consumers = new ArrayDeque<>();
  private final AtomicReference<Try<? extends T>> reference = new AtomicReference<>();

  private final Executor executor;

  PromiseImpl(Executor executor) {
    this.executor = checkNonNull(executor);
  }

  @Override
  public boolean tryComplete(Try<? extends T> value) {
    if (isEmpty()) {
      lock.lock();
      try {
        if (isEmpty()) {
          set(value);
          condition.signalAll();
          while (true) {
            var consumer = consumers.poll();
            if (consumer != null)
              submit(value, consumer);
            else break;
          }
          return true;
        }
      } finally {
        lock.unlock();
      }
    }
    return false;
  }

  @Override
  public Try<T> await() {
    if (isEmpty()) {
      lock.lock();
      try {
        while (isEmpty()) {
          condition.await();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        set(Try.failure(e));
      } finally {
        lock.unlock();
      }
    }
    return safeGet();
  }

  @Override
  public Try<T> await(Duration timeout) {
    if (isEmpty()) {
      lock.lock();
      try {
        if (isEmpty() && !condition.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
          set(Try.failure(new TimeoutException()));
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        set(Try.failure(e));
      } finally {
        lock.unlock();
      }
    }
    return safeGet();
  }

  @Override
  public boolean isCompleted() {
    lock.lock();
    try {
      return !isEmpty();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Promise<T> onComplete(Consumer1<? super Try<? extends T>> consumer) {
    current(consumer).ifPresent(consumer);
    return this;
  }

  @Override
  public <R> Promise<R> ap(Kind<Promise<?>, ? extends Function1<? super T, ? extends R>> apply) {
    Promise<R> result = new PromiseImpl<>(executor);
    onComplete(try1 -> PromiseOf.toPromise(apply).onComplete(
        try2 -> result.tryComplete(Try.map2(try2,  try1, Function1::apply))));
    return result;
  }

  @Override
  public <R> Promise<R> map(Function1<? super T, ? extends R> mapper) {
    Promise<R> other = new PromiseImpl<>(executor);
    onComplete(value -> other.tryComplete(value.map(mapper)));
    return other;
  }

  @Override
  public <R> Promise<R> flatMap(Function1<? super T, ? extends Kind<Promise<?>, ? extends R>> mapper) {
    Promise<R> other = new PromiseImpl<>(executor);
    onComplete(value -> {
      Try<Promise<R>> map = value.map(mapper.andThen(PromiseOf::toPromise));
      map.fold(
        error -> other.tryComplete(Try.failure(error)),
        next -> next.onComplete(other::tryComplete));
    });
    return other;
  }

  private Option<Try<T>> current(Consumer1<? super Try<? extends T>> consumer) {
    if (isEmpty()) {
      lock.lock();
      try {
        if (isEmpty()) {
          consumers.add(consumer);
        }
      } finally {
        lock.unlock();
      }
    }
    return Option.of(safeGet());
  }

  @SuppressWarnings("NullAway")
  private Try<T> safeGet() {
    return TryOf.toTry(reference.get());
  }

  private void set(Try<? extends T> value) {
    reference.set(value);
  }

  private boolean isEmpty() {
    return reference.get() == null;
  }

  private void submit(Try<? extends T> value, Consumer1<? super Try<? extends T>> consumer) {
    executor.execute(() -> consumer.accept(value));
  }
}
