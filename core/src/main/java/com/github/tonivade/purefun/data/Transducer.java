/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;

/**
 * Transducer is a higher-order function that takes a reducer and returns a new reducer.
 * It allows you to compose transformations on data without creating intermediate collections.
 *
 * @param <A> The type of the accumulator (e.g., List, Set, etc.)
 * @param <T> The type of the input elements
 * @param <U> The type of the output elements after transformation
 */
public interface Transducer<A, T, U> {

  /**
   * Applies this transducer to a given reducer, returning a new reducer that incorporates the transformation logic.
   *
   * @param reducer The original reducer to which the transducer will be applied
   * @return A new reducer
   */
  Reducer<A, T> apply(Reducer<A, U> reducer);

  @SuppressWarnings("unchecked")
  default <R extends Iterable<U>> Transducer<R, T, U> narrowK() {
    return (Transducer<R, T, U>) this;
  }

  /**
   * Composes two transducers into a single transducer that applies both transformations in sequence.
   *
   * @param t1 The first transducer to apply
   * @param t2 The second transducer to apply after the first one
   * @return A new transducer that represents the composition of t1 and t2
   */
  static <A, T, U, V> Transducer<A, T, V> compose(
      Transducer<A, T, U> t1,
      Transducer<A, U, V> t2) {
    return reducer -> t1.apply(t2.apply(reducer));
  }

  /**
   * Creates a transducer that applies a mapping function to each input element before passing it to the reducer.
   *
   * @param f The mapping function to apply to each input element
   * @return A new transducer that applies the mapping function
   */
  static <A, T, U> Transducer<A, T, U> map(Function1<? super T, ? extends U> f) {
    return reducer ->
        (acc, value) -> reducer.apply(acc, f.apply(value));
  }

  /**
   * Creates a transducer that applies a flat-mapping function to each input element, allowing for multiple output elements per input.
   *
   * @param f The flat-mapping function that returns a sequence of output elements for each input element
   * @return A new transducer that applies the flat-mapping function
   */
  static <A, T, U> Transducer<A, T, U> flatMap(Function1<? super T, ? extends Sequence<U>> f) {
    return reducer ->
        (acc, value) -> {
            for (var u : f.apply(value)) {
                acc = reducer.apply(acc, u);
            }
            return acc;
        };
  }

  /**
   * Creates a transducer that filters input elements based on a predicate, only passing those that satisfy the predicate to the reducer.
   *
   * @param p The predicate function that determines whether an input element should be included
   * @return A new transducer that applies the filtering logic
   */
  static <A, T> Transducer<A, T, T> filter(Matcher1<? super T> p) {
    return reducer ->
        (acc, value) -> p.test(value)
            ? reducer.apply(acc, value)
            : acc;
  }

  /**
   * Transduces the input data using the provided transducer and reducer, starting with an initial accumulator value.
   *
   * @param transducer The transducer to apply to the input data
   * @param reducer The reducer that will be used to accumulate the results
   * @param init The initial value of the accumulator
   * @param input The input data to be transduced
   * @return The final accumulated result after transduction
   */
  static <A extends Iterable<U>, T, U> A transduce(
      Transducer<A, T, U> transducer, Reducer<A, U> reducer, A init, Iterable<T> input) {
    var r = transducer.apply(reducer);
    var acc = init;
    for (var value : input) {
      acc = r.apply(acc, value);
    }
    return acc;
  }
}
