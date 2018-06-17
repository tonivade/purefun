/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.Map;

public interface Tuple {
  
  Sequence<Object> toSequence();

  static <A> Tuple1<A> of(A value1) {
    return Tuple1.of(value1);
  }

  static <A, B> Tuple2<A, B> of(A value1, B value2) {
    return Tuple2.of(value1, value2);
  }

  static <A, B, C> Tuple3<A, B, C> of(A value1, B value2, C value3) {
    return Tuple3.of(value1, value2, value3);
  }

  static <A, B, C, D> Tuple4<A, B, C, D> of(A value1, B value2, C value3, D value4) {
    return Tuple4.of(value1, value2, value3, value4);
  }

  static <A, B, C, D, E> Tuple5<A, B, C, D, E> of(A value1, B value2, C value3, D value4, E value5) {
    return Tuple5.of(value1, value2, value3, value4, value5);
  }

  static <A, B> Tuple2<A, B> from(Map.Entry<A, B> entry) {
    return Tuple2.of(entry.getKey(), entry.getValue());
  }
}
