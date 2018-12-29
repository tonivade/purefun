/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;

@FunctionalInterface
public interface Consumer2<A, B> {
  
  void accept(A value1, B value2);
  
  default Consumer2<A, B> andThen(Consumer2<A, B> after) {
    return (value1, value2) -> { accept(value1, value2); after.accept(value1, value2); };
  }
  
  default Function2<A, B, Nothing> asFunction() {
    return (value1, value2) -> { accept(value1, value2); return nothing(); };
  }
  
  static <A, B> Consumer2<A, B> of(Consumer2<A, B> reference) {
    return reference;
  }
}
