/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface Consumer3<A, B, C> extends Recoverable {

  default void accept(A value1, B value2, C value3) {
    try {
      run(value1, value2, value3);
    } catch (Throwable t) {
      sneakyThrow(t);
    }
  }

  void run(A value1, B value2, C value3) throws Throwable;
}
