/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Unit.unit;

import java.util.function.Consumer;

/**
 * <p>This interface represents a function that receives a single parameter but it doesn't generate any result.</p>
 * <p>It's like a {@code Function1<A, Unit>}</p>
 * @param <A> the type of parameter received by the function
 */
@FunctionalInterface
public interface Consumer1<A> extends Recoverable, Consumer<A> {

  @Override
  default void accept(A value) {
    try {
      run(value);
    } catch (Throwable t) {
      sneakyThrow(t);
    }
  }

  void run(A value) throws Throwable;

  default Function1<A, Unit> asFunction() {
    return value -> { accept(value); return unit(); };
  }

  default Consumer1<A> andThen(Consumer1<? super A> after) {
    return value -> { accept(value); after.accept(value); };
  }

  default Function1<A, A> peek() {
    return value -> { accept(value); return value; };
  }

  @SuppressWarnings("unchecked")
  static <A> Consumer1<A> of(Consumer1<? super A> reference) {
    return (Consumer1<A>) reference;
  }

  static <A> Consumer1<A> noop() {
    return value -> { /* noop */ };
  }
}
