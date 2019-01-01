/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;

@FunctionalInterface
public interface CheckedRunnable extends Recoverable {

  void run() throws Exception;

  default CheckedProducer<Nothing> asProducer() {
    return () -> { run(); return nothing(); };
  }

  default Runnable recover(Consumer1<Throwable> mapper) {
    return () -> {
      try {
        run();
      } catch(Exception e) {
        mapper.accept(e);
      }
    };
  }

  default Runnable unchecked() {
    return recover(this::sneakyThrow);
  }

  static <X extends Exception> CheckedRunnable failure(Producer<X> supplier) {
    return () -> { throw supplier.get(); };
  }

  static CheckedRunnable of(CheckedRunnable runnable) {
    return runnable;
  }
}