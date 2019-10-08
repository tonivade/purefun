/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

/**
 * <p>This interface represents a {@link Runnable} instance but it can throws any exception.</p>
 */
@FunctionalInterface
public interface CheckedRunnable extends Recoverable {

  void run() throws Throwable;

  default Producer<Unit> asProducer() {
    return () -> { run(); return Unit.unit(); };
  }

  default CheckedRunnable andThen(CheckedRunnable next) {
    return () -> { run(); next.run(); };
  }

  default Runnable recover(Consumer1<Throwable> mapper) {
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

  static <X extends Throwable> CheckedRunnable failure(Producer<X> supplier) {
    return () -> { throw supplier.get(); };
  }

  static CheckedRunnable of(CheckedRunnable runnable) {
    return runnable;
  }
}