/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface CheckedFunction2<T, V, R> {

  R apply(T t, V v) throws Throwable;

  default CheckedFunction1<T, CheckedFunction1<V, R>> curried() {
    return t -> v -> apply(t, v);
  }

  default CheckedFunction1<Tuple2<T, V>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2());
  }

  default <U> CheckedFunction2<T, V, U> andThen(CheckedFunction1<R, U> after) {
    return (t, v) -> after.apply(apply(t, v));
  }

  default <U> CheckedFunction1<U, R> compose(CheckedFunction1<U, T> beforeT, CheckedFunction1<U, V> beforeV) {
    return value -> apply(beforeT.apply(value), beforeV.apply(value));
  }

  static <T, V, R> CheckedFunction2<T, V, R> of(CheckedFunction2<T, V, R> reference) {
    return reference;
  }
}
