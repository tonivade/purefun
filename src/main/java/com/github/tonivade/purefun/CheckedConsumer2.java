/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;

@FunctionalInterface
public interface CheckedConsumer2<A, B> extends Recoverable {

  void accept(A value1, B value2) throws Exception;

  default Consumer2<A, B> unchecked() {
    return (a, b) -> {
      try {
        accept(a, b);
      } catch (Exception e) {
        sneakyThrow(e);
      }
    };
  }

  default CheckedFunction2<A, B, Tuple2<A, B>> peek() {
    return (a, b) -> { accept(a, b); return Tuple.of(a, b); };
  }

  default CheckedConsumer2<A, B> andThen(CheckedConsumer2<A, B> after) {
    return (value1, value2) -> { accept(value1, value2); after.accept(value1, value2); };
  }

  default CheckedFunction2<A, B, Nothing> asFunction() {
    return (value1, value2) -> { accept(value1, value2); return nothing(); };
  }

  static <T, V> CheckedConsumer2<T, V> of(CheckedConsumer2<T, V> reference) {
    return reference;
  }
}
