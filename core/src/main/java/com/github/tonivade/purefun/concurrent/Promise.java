/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

interface Promise<T> {
  
  void begin();

  void complete(Try<T> value);

  default void succeced(T value) {
    complete(Try.success(value));
  }

  default void errored(Throwable error) {
    complete(Try.failure(error));
  }

  void onComplete(Consumer1<Try<T>> consumer);

  Try<T> get();
  Try<T> get(Duration timeout);

  void cancel(boolean mayInterruptThread);

  boolean isCancelled();
  boolean isCompleted();
  
  static <T> Promise<T> make() {
    return new PromiseImpl<>();
  }
}

final class PromiseImpl<T> implements Promise<T> {

  private final State state = new State();
  private final Queue<Consumer1<Try<T>>> consumers = new LinkedList<>();
  private final AtomicReference<Try<T>> reference = new AtomicReference<>();
  
  @Override
  public void complete(Try<T> value) {
    if (isEmpty()) {
      synchronized (state) {
        if (isEmpty()) {
          state.completed = true;
          setValue(value);
          state.notifyAll();
        }
      }
    }
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
  public void cancel(boolean mayInterruptThread) {
    if (isEmpty()) {
      synchronized (state) {
        if (isEmpty()) {
          state.cancelled = true;
          if (mayInterruptThread) {
            state.interrupt();
          }
          setValue(Try.failure(new CancellationException()));
          state.notifyAll();
        }
      }
    }
  }

  @Override
  public boolean isCancelled() {
    synchronized (state) {
      return state.cancelled;
    }
  }

  @Override
  public boolean isCompleted() {
    synchronized (state) {
      return state.completed;
    }
  }

  @Override
  public void begin() {
    synchronized (state) {
      state.thread = Thread.currentThread();
      state.thread.setUncaughtExceptionHandler((t, ex) -> errored(ex));
    }
  }

  @Override
  public void onComplete(Consumer1<Try<T>> consumer) {
    tryOnComplete(consumer).ifPresent(consumer);
  }

  private Option<Try<T>> tryOnComplete(Consumer1<Try<T>> consumer) {
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
    consumers.forEach(consumer -> consumer.accept(value));
  }

  private boolean isEmpty() {
    return isNull(reference.get());
  }

  private static final class State {
    boolean cancelled = false;
    boolean completed = false;
    Thread thread = null;

    void interrupt() {
      if (nonNull(thread)) {
        thread.interrupt();
      }
    }
  }
}
