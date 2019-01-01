/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;

@FunctionalInterface
public interface CheckedConsumer1<A> extends Recoverable {

  void accept(A value) throws Exception;

  default CheckedFunction1<A, Nothing> asFunction() {
    return value -> { accept(value); return nothing(); };
  }

  default CheckedConsumer1<A> andThen(CheckedConsumer1<A> after) {
    return value -> { accept(value); after.accept(value); };
  }

  default Consumer1<A> unchecked() {
    return value -> {
      try {
        accept(value);
      } catch (Exception e) {
        sneakyThrow(e);
      }
    };
  }

  default CheckedFunction1<A, A> peek() {
    return value -> { accept(value); return value; };
  }

  static <A> CheckedConsumer1<A> of(CheckedConsumer1<A> reference) {
    return reference;
  }
}
