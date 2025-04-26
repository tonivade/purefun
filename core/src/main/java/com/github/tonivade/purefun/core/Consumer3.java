/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Unit.unit;

/**
 * <p>This interface represents a function that receives three parameters but it doesn't generate any result.</p>
 * <p>It's like a {@code Function3<A, B, C, Unit>}</p>
 * @param <A> the type of first parameter received by the function
 * @param <B> the type of second parameter received by the function
 * @param <C> the type of third parameter received by the function
 */
@FunctionalInterface
public interface Consumer3<A, B, C> extends Recoverable {

  default void accept(A value1, B value2, C value3) {
    try {
      run(value1, value2, value3);
    } catch (Throwable t) {
      sneakyThrow(t);
    }
  }

  void run(A value1, B value2, C value3) throws Throwable;

  default Consumer3<A, B, C> andThen(Consumer3<? super A, ? super B, ? super C> after) {
    return (value1, value2, value3) -> { accept(value1, value2, value3); after.accept(value1, value2, value3); };
  }

  default Function3<A, B, C, Unit> asFunction() {
    return (value1, value2, value3) -> { accept(value1, value2, value3); return unit(); };
  }

  @SuppressWarnings("unchecked")
  static <A, B, C> Consumer3<A, B, C> of(Consumer3<? super A, ? super B, ? super C> reference) {
    return (Consumer3<A, B, C>) reference;
  }
}
