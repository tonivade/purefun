/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

@HigherKind(sealed = true)
public interface Promise<T> extends PromiseOf<T> {

  boolean tryComplete(Try<? extends T> value);

  default Promise<T> complete(Try<? extends T> value) {
    if (tryComplete(value)) {
      return this;
    } else throw new IllegalStateException("promise already completed");
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
  
  <R> Promise<R> map(Function1<? super T, ? extends R> mapper);

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
}

final class PromiseImpl<T> implements SealedPromise<T> {

  private final State state = new State();
  private final Queue<Consumer1<? super Try<? extends T>>> consumers = new LinkedList<>();
  private final AtomicReference<Try<? extends T>> reference = new AtomicReference<>();

  private final Executor executor;

  PromiseImpl(Executor executor) {
    this.executor = checkNonNull(executor);
  }

  @Override
  public boolean tryComplete(Try<? extends T> value) {
    if (isEmpty()) {
      synchronized (state) {
        if (isEmpty()) {
          state.completed = true;
          setValue(value);
          state.notifyAll();
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Try<T> await() {
    if (isEmpty()) {
      synchronized (state) {
        if (isEmpty()) {
          try {
            state.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Try.failure(e);
          }
        }
      }
    }
    return TryOf.narrowK(checkNonNull(reference.get()));
  }

  @Override
  public Try<T> await(Duration timeout) {
    if (isEmpty()) {
      synchronized (state) {
        if (isEmpty()) {
          try {
            state.wait(timeout.toMillis());
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Try.failure(e);
          }
        }
      }
    }
    Option<Try<T>> option = Option.of(reference::get).map(TryOf::narrowK);
    return option.getOrElse(Try.<T>failure(new TimeoutException()));
  }

  @Override
  public boolean isCompleted() {
    synchronized (state) {
      return state.completed;
    }
  }

  @Override
  public Promise<T> onComplete(Consumer1<? super Try<? extends T>> consumer) {
    current(consumer).ifPresent(consumer);
    return this;
  }
  
  @Override
  public <R> Promise<R> map(Function1<? super T, ? extends R> mapper) {
    Promise<R> other = new PromiseImpl<>(executor);
    onComplete(value -> other.tryComplete(value.map(mapper)));
    return other;
  }

  private Option<Try<T>> current(Consumer1<? super Try<? extends T>> consumer) {
    Try<? extends T> current = reference.get();
    if (isNull(current)) {
      synchronized (state) {
        current = reference.get();
        if (isNull(current)) {
          consumers.add(consumer);
        }
      }
    }
    return Option.of(TryOf.narrowK(current));
  }

  private void setValue(Try<? extends T> value) {
    reference.set(value);
    consumers.forEach(consumer -> submit(value, consumer));
  }

  private void submit(Try<? extends T> value, Consumer1<? super Try<? extends T>> consumer) {
    executor.execute(() -> consumer.accept(value));
  }

  private boolean isEmpty() {
    return isNull(reference.get());
  }

  private static final class State {
    private boolean completed = false;
  }
}
