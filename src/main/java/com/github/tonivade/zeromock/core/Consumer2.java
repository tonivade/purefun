/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Nothing.nothing;

@FunctionalInterface
public interface Consumer2<T, V> {
  
  void apply(T value1, V value2);
  
  default Function2<T, V, Nothing> asFunction() {
    return (value1, value2) -> { apply(value1, value2); return nothing(); };
  }
}
