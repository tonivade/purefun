/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface Function5<A, B, C, D, E, R> {

  R apply(A a, B b, C c, D d, E e);

  default Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, R>>>>> curried() {
    return a -> b -> c -> d -> e -> apply(a, b, c, d, e);
  }

  default Function1<Tuple5<A, B, C, D, E>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2(), tuple.get3(), tuple.get4(), tuple.get5());
  }

  default <U> Function5<A, B, C, D, E, U> andThen(Function1<R, U> after) {
    return (a, b, c, d, e) -> after.apply(apply(a, b, c, d, e));
  }

  default <U> Function1<U, R> compose(Function1<U, A> beforeT1, Function1<U, B> beforeT2,
      Function1<U, C> beforeT3, Function1<U, D> beforeT4, Function1<U, E> beforeT5) {
    return value -> apply(beforeT1.apply(value), beforeT2.apply(value),
        beforeT3.apply(value), beforeT4.apply(value), beforeT5.apply(value));
  }

  default Function5<A, B, C, D, E, R> memoized() {
    return (a, b, c, d, e) -> new MemoizedFunction<>(tupled()).apply(Tuple.of(a, b, c, d, e));
  }
}

