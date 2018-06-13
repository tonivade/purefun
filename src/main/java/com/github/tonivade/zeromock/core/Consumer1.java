/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Nothing.nothing;

import java.util.function.Consumer;

@FunctionalInterface
public interface Consumer1<T> {

  void apply(T value);
  
  default Handler1<T, Nothing> toHandler1() {
    return value -> { apply(value); return nothing(); };
  }
  
  default Handler1<T, T> bypass() {
    return value -> { apply(value); return value; };
  }
  
  static <T> Consumer1<T> adapt(Consumer<T> consumer) {
    return consumer::accept;
  }
}
