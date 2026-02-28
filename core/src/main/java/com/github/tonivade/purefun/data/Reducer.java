/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

/**
 * Reducer is a functional interface that represents a function that takes an accumulator and an element, and returns a new accumulator.
 * It is used in the context of pipelines to define how to combine elements into a result.
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
   * @return A Step that indicates whether the reduction should continue (More) or is complete (Done), along with the new accumulator value
   */
  Step<A> apply(A accumulator, E element);

  /**
   * Step is a sealed interface that represents the result of applying a reducer. It can be either a "More" step, which indicates that the reduction should continue with the new accumulator, or a "Done" step, which indicates that the reduction is complete and provides the final result.
   *
   * @param <A> The type of the accumulator
   */
  sealed interface Step<A> {

    /**
     * More is a record that represents a step in the reduction process where the reduction should continue with the new accumulator value.
     *
     * @param value The new accumulator value for the next step
     * @param <A> The type of the accumulator
     */
    record More<A>(A value) implements Step<A> {}

    /**
     * Done is a record that represents a step in the reduction process where the reduction is complete and provides the final result.
     *
     * @param value The final accumulator value
     * @param <A> The type of the accumulator
     */
    record Done<A>(A value) implements Step<A> {}

    /**
     * Retrieves the value contained in this Step, whether it is a More or Done step.
     *
     * @return The value of the accumulator at this step
     */
    A value();

    /**
     * Creates a More step with the given value, indicating that the reduction should continue.
     *
     * @param value The new accumulator value for the next step
     * @param <A> The type of the accumulator
     * @return A More step containing the new accumulator value
     */
    static <A> More<A> more(A value) {
      return new More<>(value);
    }

    /**
     * Creates a Done step with the given value, indicating that the reduction is complete.
     *
     * @param value The final accumulator value
     * @param <A> The type of the accumulator
     * @return A Done step containing the final accumulator value
     */
    static <A> Done<A> done(A value) {
      return new Done<>(value);
    }
  }
}
