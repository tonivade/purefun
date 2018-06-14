/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Nothing.nothing;

@FunctionalInterface
public interface Consumer1<T> {

  void apply(T value);
  
  default Function1<T, Nothing> asFunction() {
    return value -> { apply(value); return nothing(); };
  }
  
  default Function1<T, T> bypass() {
    return value -> { apply(value); return value; };
  }
}
