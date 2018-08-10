/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;

@FunctionalInterface
public interface CheckedConsumer1<T> {

  void accept(T value) throws Exception;
  
  default CheckedFunction1<T, Nothing> asFunction() {
    return value -> { accept(value); return nothing(); };
  }
  
  default CheckedConsumer1<T> andThen(CheckedConsumer1<T> after) {
    return value -> { accept(value); after.accept(value); };
  }
  
  default CheckedFunction1<T, T> peek() {
    return value -> { accept(value); return value; };
  }
  
  static <T> CheckedConsumer1<T> of(CheckedConsumer1<T> reference) {
    return reference;
  }

}
