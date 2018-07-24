/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;

@FunctionalInterface
public interface Consumer1<T> {

  void accept(T value);
  
  default Function1<T, Nothing> asFunction() {
    return value -> { accept(value); return nothing(); };
  }
  
  default Consumer1<T> andThen(Consumer1<T> after) {
    return value -> { accept(value); after.accept(value); };
  }
  
  default Function1<T, T> bypass() {
    return value -> { accept(value); return value; };
  }
  
  static <T> Consumer1<T> of(Consumer1<T> reference) {
    return reference;
  }
}
