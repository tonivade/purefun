/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Nothing.nothing;

@FunctionalInterface
public interface Consumer2<T, V> {
  
  void accept(T value1, V value2);
  
  default Consumer2<T, V> andThen(Consumer2<T, V> after) {
    return (value1, value2) -> { accept(value1, value2); after.accept(value1, value2); };
  }
  
  default Function2<T, V, Nothing> asFunction() {
    return (value1, value2) -> { accept(value1, value2); return nothing(); };
  }
}
