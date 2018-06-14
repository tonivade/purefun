/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.function.Supplier;

@FunctionalInterface
public interface Producer<T> {
  
  T get();
  
  default <V> Function1<V, T> asFunction() {
    return value -> get();
  }
  
  static <T> Producer<T> unit(T value) {
    return () -> value;
  }

  static <T> Producer<T> adapt(Supplier<T> supplier) {
    return supplier::get;
  }
}
