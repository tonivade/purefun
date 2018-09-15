/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface Function3<A, B, C, R> {

  R apply(A t1, B t2, C t3);

  default Function1<A, Function1<B, Function1<C, R>>> curried() {
    return t1 -> t2 -> t3 -> apply(t1, t2, t3);
  }

  default Function1<Tuple3<A, B, C>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2(), tuple.get3());
  }

  default <U> Function3<A, B, C, U> andThen(Function1<R, U> after) {
    return (t1, t2, t3) -> after.apply(apply(t1, t2, t3));
  }

  default <U> Function1<U, R> compose(Function1<U, A> beforeT1, Function1<U, B> beforeT2, Function1<U, C> beforeT3) {
    return value -> apply(beforeT1.apply(value), beforeT2.apply(value), beforeT3.apply(value));
  }
}
