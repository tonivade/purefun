/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface Function5<A, B, C, D, E, R> {

  R apply(A t1, B t2, C t3, D t4, E t5);

  default Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, R>>>>> curried() {
    return t1 -> t2 -> t3 -> t4 -> t5 -> apply(t1, t2, t3, t4, t5);
  }

  default Function1<Tuple5<A, B, C, D, E>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2(), tuple.get3(), tuple.get4(), tuple.get5());
  }

  default <U> Function5<A, B, C, D, E, U> andThen(Function1<R, U> after) {
    return (t1, t2, t3, t4, t5) -> after.apply(apply(t1, t2, t3, t4, t5));
  }

  default <U> Function1<U, R> compose(Function1<U, A> beforeT1, Function1<U, B> beforeT2,
      Function1<U, C> beforeT3, Function1<U, D> beforeT4, Function1<U, E> beforeT5) {
    return value -> apply(beforeT1.apply(value), beforeT2.apply(value),
        beforeT3.apply(value), beforeT4.apply(value), beforeT5.apply(value));
  }
}

