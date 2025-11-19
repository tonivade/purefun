/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Unit.unit;

import java.util.function.BiConsumer;

/**
 * <p>This interface represents a function that receives two parameters but it doesn't generate any result.</p>
 * <p>It's like a {@code Function2<A, B, Unit>}</p>
 * @param <A> the type of first parameter received by the function
 * @param <B> the type of second parameter received by the function
 */
@FunctionalInterface
public interface Consumer2<A, B> extends Recoverable, BiConsumer<A, B> {

  @Override
  default void accept(A value1, B value2) {
    try {
      run(value1, value2);
    } catch (Throwable t) {
      sneakyThrow(t);
    }
  }

  void run(A value1, B value2) throws Throwable;

  default Consumer2<A, B> andThen(Consumer2<? super A, ? super B> after) {
    return (value1, value2) -> { accept(value1, value2); after.accept(value1, value2); };
  }

  default Function2<A, B, Unit> asFunction() {
    return (value1, value2) -> { accept(value1, value2); return unit(); };
  }

  @SuppressWarnings("unchecked")
  static <A, B> Consumer2<A, B> of(Consumer2<? super A, ? super B> reference) {
    return (Consumer2<A, B>) reference;
  }
}
