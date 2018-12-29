/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface CheckedFunction2<A, B, R> {

  R apply(A t, B v) throws Throwable;

  default CheckedFunction1<A, CheckedFunction1<B, R>> curried() {
    return t -> v -> apply(t, v);
  }

  default CheckedFunction1<Tuple2<A, B>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2());
  }

  default <C> CheckedFunction2<A, B, C> andThen(CheckedFunction1<R, C> after) {
    return (t, v) -> after.apply(apply(t, v));
  }

  default <C> CheckedFunction1<C, R> compose(CheckedFunction1<C, A> beforeT, CheckedFunction1<C, B> beforeV) {
    return value -> apply(beforeT.apply(value), beforeV.apply(value));
  }

  static <A, B, R> CheckedFunction2<A, B, R> of(CheckedFunction2<A, B, R> reference) {
    return reference;
  }
}
