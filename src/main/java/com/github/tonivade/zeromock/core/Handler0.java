/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.function.Supplier;

public interface Handler0<T> {
  
  T handle();

  default <V> Handler1<V, T> toHandler1() {
    return value -> handle();
  }
  
  static <T> Handler0<T> adapt(Supplier<T> supplier) {
    return () -> supplier.get();
  }
}
