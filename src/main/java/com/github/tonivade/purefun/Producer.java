/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface Producer<T> {
  
  T get();
  
  default <V> Function1<V, T> asFunction() {
    return value -> get();
  }
  
  default <R> Producer<R> andThen(Function1<T, R> after) {
    return () -> after.apply(get());
  }
  
  static <T> Producer<T> unit(T value) {
    return () -> value;
  }
  
  static <T> Producer<T> of(Producer<T> reference) {
    return reference;
  }
}
