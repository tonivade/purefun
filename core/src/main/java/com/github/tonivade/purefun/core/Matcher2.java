/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

@FunctionalInterface
public interface Matcher2<A, B> extends Recoverable {

  default boolean match(A a, B b) {
    try {
      return run(a, b);
    } catch (Throwable t) {
      return sneakyThrow(t);
    }
  }

  boolean run(A a, B b) throws Throwable;

  default Matcher1<Tuple2<A, B>> tupled() {
    return tuple -> match(tuple.get1(), tuple.get2());
  }

  static <A, B> Matcher2<A, B> invalid() {
    return (a, b) -> { throw new IllegalStateException(); };
  }

  static <A, B> Matcher2<A, B> otherwise() {
    return (a, b) -> true;
  }
}
