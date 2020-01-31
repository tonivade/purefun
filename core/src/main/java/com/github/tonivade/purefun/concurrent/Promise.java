/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public interface Promise<T> {

  boolean tryComplete(Try<T> value);

  default Promise<T> complete(Try<T> value) {
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

  Promise<T> onComplete(Consumer1<Try<T>> consumer);

  default Promise<T> onSuccess(Consumer1<T> consumer) {
    return onComplete(value -> value.onSuccess(consumer));
  }

  default Promise<T> onFailure(Consumer1<Throwable> consumer) {
    return onComplete(value -> value.onFailure(consumer));
  }

  Try<T> get();
  Try<T> get(Duration timeout);

  boolean isCompleted();

  static <T> Promise<T> make() {
    return make(Future.DEFAULT_EXECUTOR);
  }

  static <T> Promise<T> make(Executor executor) {
    return new PromiseImpl<>(executor);
  }

  static <T> Promise<T> from(CompletableFuture<T> future) {
    return from(Future.DEFAULT_EXECUTOR, future);
  }

  static <T> Promise<T> from(Executor executor, CompletableFuture<T> future) {
    Promise<T> promise = make(executor);
    future.whenCompleteAsync((unit, error) -> {
      if (unit != null) {
        promise.tryComplete(Try.success(unit));
      } else {
        promise.tryComplete(Try.failure(error));
      }
    }, executor);
    return promise;
  }
}

final class PromiseImpl<T> implements Promise<T> {

  private final State state = new State();
  private final Queue<Consumer1<Try<T>>> consumers = new LinkedList<>();
  private final AtomicReference<Try<T>> reference = new AtomicReference<>();

  private final Executor executor;

  PromiseImpl(Executor executor) {
    this.executor = requireNonNull(executor);
  }

  @Override
  public boolean tryComplete(Try<T> value) {
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
  public Try<T> get() {
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
    return requireNonNull(reference.get());
  }

  @Override
  public Try<T> get(Duration timeout) {
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
    return Option.of(reference::get).getOrElse(Try.failure(new TimeoutException()));
  }

  @Override
  public boolean isCompleted() {
    synchronized (state) {
      return state.completed;
    }
  }

  @Override
  public Promise<T> onComplete(Consumer1<Try<T>> consumer) {
    current(consumer).ifPresent(consumer);
    return this;
  }

  private Option<Try<T>> current(Consumer1<Try<T>> consumer) {
    Try<T> current = reference.get();
    if (isNull(current)) {
      synchronized (state) {
        current = reference.get();
        if (isNull(current)) {
          consumers.add(consumer);
        }
      }
    }
    return Option.of(current);
  }

  private void setValue(Try<T> value) {
    reference.set(value);
    consumers.forEach(consumer -> submit(value, consumer));
  }

  private void submit(Try<T> value, Consumer1<Try<T>> consumer) {
    executor.execute(() -> consumer.accept(value));
  }

  private boolean isEmpty() {
    return isNull(reference.get());
  }

  private static final class State {
    private boolean completed = false;
  }
}
