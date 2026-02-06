/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

/**
 * Reducer is a functional interface that represents a function that takes an accumulator and an element, and returns a new accumulator.
 * It is used in the context of transducers to define how to combine elements into a result.
 *
 * @param <A> The type of the accumulator (e.g., List, Set, etc.)
 * @param <E> The type of the input elements
 */
@FunctionalInterface
public interface Reducer<A, E> {

  /**
   * Applies this reducer to a given accumulator and an element, returning a new accumulator that incorporates the element.
   *
   * @param accumulator The current state of the accumulator
   * @param element The element to be added to the accumulator
   * @return A new accumulator that includes the element
   */
  Step<A> apply(A accumulator, E element);

  sealed interface Step<A> {

    record More<A>(A value) implements Step<A> {}
    record Done<A>(A value) implements Step<A> {}

    A value();

    static <A> More<A> more(A value) {
      return new More<>(value);
    }

    static <A> Done<A> done(A value) {
      return new Done<>(value);
    }
  }
}
