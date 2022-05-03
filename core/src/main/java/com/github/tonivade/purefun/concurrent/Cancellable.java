/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static java.util.Objects.nonNull;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.ReentrantLock;
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
  
  private final ReentrantLock lock = new ReentrantLock(true);
  private boolean cancelled = false;
  private Thread thread = null;

  private final Promise<?> promise;
  
  public CancellableImpl(Promise<?> promise) {
    this.promise = checkNonNull(promise);
  }

  @Override
  public void updateThread() {
    try {
      lock.lock();
      thread = Thread.currentThread();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void cancel(boolean mayThreadInterrupted) {
    if (promise.tryComplete(Try.failure(new CancellationException()))) {
      try {
        lock.lock();
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
    try {
      lock.lock();
      return cancelled;
    } finally {
      lock.unlock();
    }
  }
  
  private void interrupt() {
    if (nonNull(thread)) {
      thread.interrupt();
      thread = null;
    }
  }
}
