/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static java.util.Objects.nonNull;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.util.concurrent.CancellationException;

import com.github.tonivade.purefun.type.Try;

public interface Cancellable {
  
  void updateThread();
  
  void cancel(boolean mayTreadInterrupted);

  boolean isCancelled();
  
  static Cancellable from(Promise<?> promise) {
    return new CancellableImpl(promise);
  }
}

final class CancellableImpl implements Cancellable {
  
  private final State state = new State();
  private final Promise<?> promise;
  
  public CancellableImpl(Promise<?> promise) {
    this.promise = checkNonNull(promise);
  }

  @Override
  public void updateThread() {
    synchronized (state) {
      state.thread = Thread.currentThread();
    }
  }

  @Override
  public void cancel(boolean mayThreadInterrupted) {
    if (promise.tryComplete(Try.failure(new CancellationException()))) {
      synchronized (state) {
        state.cancelled = true;
        if (mayThreadInterrupted) {
          state.interrupt();
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
  
  private static final class State {
    private boolean cancelled = false;
    private Thread thread = null;

    private void interrupt() {
      if (nonNull(thread)) {
        thread.interrupt();
        thread = null;
      }
    }
  }
}
