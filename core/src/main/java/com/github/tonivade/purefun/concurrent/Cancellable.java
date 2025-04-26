/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.ReentrantLock;

import com.github.tonivade.purefun.Nullable;
import com.github.tonivade.purefun.type.Try;

public sealed interface Cancellable {

  void updateThread();

  void cancel(boolean mayTreadInterrupted);

  boolean isCancelled();

  static Cancellable from(Promise<?> promise) {
    return new CancellableImpl(promise);
  }
}

final class CancellableImpl implements Cancellable {

  private final ReentrantLock lock = new ReentrantLock();
  private boolean cancelled = false;
  @Nullable
  private Thread thread = null;

  private final Promise<?> promise;

  public CancellableImpl(Promise<?> promise) {
    this.promise = checkNonNull(promise);
  }

  @Override
  public void updateThread() {
    lock.lock();
    try {
      thread = Thread.currentThread();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void cancel(boolean mayThreadInterrupted) {
    if (promise.tryComplete(Try.failure(new CancellationException()))) {
      lock.lock();
      try {
        cancelled = true;
        if (mayThreadInterrupted) {
          interrupt();
        }
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  public boolean isCancelled() {
    lock.lock();
    try {
      return cancelled;
    } finally {
      lock.unlock();
    }
  }

  private void interrupt() {
    if (thread != null) {
      thread.interrupt();
      thread = null;
    }
  }
}
