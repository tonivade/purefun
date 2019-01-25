/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface CheckedFunction2<A, B, R> {

  R apply(A a, B b) throws Throwable;

  default CheckedFunction1<A, CheckedFunction1<B, R>> curried() {
    return a -> b -> apply(a, b);
  }

  default CheckedFunction1<Tuple2<A, B>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2());
  }

  default <C> CheckedFunction2<A, B, C> andThen(CheckedFunction1<R, C> after) {
    return (a, b) -> after.apply(apply(a, b));
  }

  default <C> CheckedFunction1<C, R> compose(CheckedFunction1<C, A> beforeA, CheckedFunction1<C, B> beforeB) {
    return value -> apply(beforeA.apply(value), beforeB.apply(value));
  }

  static <A, B, R> CheckedFunction2<A, B, R> of(CheckedFunction2<A, B, R> reference) {
    return reference;
  }
}
