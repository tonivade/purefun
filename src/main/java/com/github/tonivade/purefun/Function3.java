/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface Function3<A, B, C, R> {

  R apply(A a, B b, C c);

  default Function1<A, Function1<B, Function1<C, R>>> curried() {
    return a -> b -> c -> apply(a, b, c);
  }

  default Function1<Tuple3<A, B, C>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2(), tuple.get3());
  }

  default <D> Function3<A, B, C, D> andThen(Function1<R, D> after) {
    return (a, b, c) -> after.apply(apply(a, b, c));
  }

  default <D> Function1<D, R> compose(Function1<D, A> beforeT1, Function1<D, B> beforeT2, Function1<D, C> beforeT3) {
    return value -> apply(beforeT1.apply(value), beforeT2.apply(value), beforeT3.apply(value));
  }

  default Function3<A, B, C, R> memoized() {
    return (a, b, c) -> new MemoizedFunction<>(tupled()).apply(Tuple.of(a, b, c));
  }
}
