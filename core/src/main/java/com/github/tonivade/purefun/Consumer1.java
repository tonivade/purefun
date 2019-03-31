/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;

@FunctionalInterface
public interface Consumer1<A> {

  void accept(A value);
  
  default Function1<A, Nothing> asFunction() {
    return value -> { accept(value); return nothing(); };
  }
  
  default Consumer1<A> andThen(Consumer1<A> after) {
    return value -> { accept(value); after.accept(value); };
  }
  
  default Function1<A, A> peek() {
    return value -> { accept(value); return value; };
  }
  
  static <A> Consumer1<A> of(Consumer1<A> reference) {
    return reference;
  }

  static <A> Consumer1<A> noop() {
    return value -> { /* noop */ };
  }
}
