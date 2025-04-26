/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

/**
 * <p>This interface represents a {@link Runnable} instance but it can throws any exception.</p>
 */
@FunctionalInterface
public interface CheckedRunnable extends Recoverable {

  void run() throws Throwable;
  
  default void exec() {
    try {
      run();
    } catch (Throwable e) {
      sneakyThrow(e);
    }
  }

  default Producer<Unit> asProducer() {
    return () -> { run(); return Unit.unit(); };
  }

  default CheckedRunnable andThen(CheckedRunnable next) {
    return () -> { run(); next.run(); };
  }

  default Runnable recover(Consumer1<? super Throwable> mapper) {
    return () -> {
      try {
        run();
      } catch(Throwable e) {
        mapper.accept(e);
      }
    };
  }

  default Runnable unchecked() {
    return recover(this::sneakyThrow);
  }

  static <X extends Throwable> CheckedRunnable failure(Producer<? extends X> supplier) {
    return () -> { throw supplier.get(); };
  }

  static CheckedRunnable of(CheckedRunnable runnable) {
    return runnable;
  }
}