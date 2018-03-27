/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.function.BiFunction;

@FunctionalInterface
public interface Handler2<T, V, R> {

  R handle(T t, V v);
  
  default Handler1<T, Handler1<V, R>> curried() {
    return t -> v -> handle(t, v);
  }
  
  default <U> Handler2<T, V, U> andThen(Handler1<R, U> after) {
    return (t, v) -> after.handle(handle(t, v));
  }
  
  default <U> Handler1<U, R> compose(Handler1<U, T> beforeT, Handler1<U, V> beforeV) {
    return value -> handle(beforeT.handle(value), beforeV.handle(value));
  }
  
  static <T, V, R> Handler2<T, V, R> adapt(BiFunction<T, V, R> handler) {
    return handler::apply;
  }
}
