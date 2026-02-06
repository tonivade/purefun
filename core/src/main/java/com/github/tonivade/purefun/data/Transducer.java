/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.data.Reducer.Step;

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
  static <A, T, U, V> Transducer<A, T, V> compose(Transducer<A, T, U> t1, Transducer<A, U, V> t2) {
    return reducer -> t1.apply(t2.apply(reducer));
  }

  /**
   * Composes three transducers into a single transducer that applies all three transformations in sequence.
   *
   * @param t1 The first transducer to apply
   * @param t2 The second transducer to apply after the first one
   * @param t3 The third transducer to apply after the second one
   * @return A new transducer that represents the composition of t1, t2, and t3
   */
  static <A, T, U, V, W> Transducer<A, T, W> compose(
      Transducer<A, T, U> t1, Transducer<A, U, V> t2, Transducer<A, V, W> t3) {
    return reducer -> t1.apply(t2.apply(t3.apply(reducer)));
  }

  /**
   * Creates a transducer that applies a mapping function to each input element before passing it to the reducer.
   *
   * @param mapper The mapping function to apply to each input element
   * @return A new transducer that applies the mapping function
   */
  static <A, T, U> Transducer<A, T, U> map(Function1<? super T, ? extends U> mapper) {
    return reducer ->
        (acc, value) -> reducer.apply(acc, mapper.apply(value));
  }

  /**
   * Creates a transducer that applies a flat-mapping function to each input element, allowing for multiple output elements per input.
   *
   * @param mapper The flat-mapping function that returns a sequence of output elements for each input element
   * @return A new transducer that applies the flat-mapping function
   */
  static <A, T, U> Transducer<A, T, U> flatMap(Function1<? super T, ? extends Sequence<U>> mapper) {
    return reducer ->
        (init, value) -> {
          var acc = init;
          for (var u : mapper.apply(value)) {
            var step = reducer.apply(acc, u);
            if (step instanceof Step.Done) {
              return step;
            }
            acc = step.value();
          }
          return Step.more(acc);
        };
  }

  /**
   * Creates a transducer that filters input elements based on a predicate, only passing those that satisfy the predicate to the reducer.
   *
   * @param matcher The predicate function that determines whether an input element should be included
   * @return A new transducer that applies the filtering logic
   */
  static <A, T> Transducer<A, T, T> filter(Matcher1<? super T> matcher) {
    return reducer ->
        (acc, value) -> matcher.test(value)
            ? reducer.apply(acc, value)
            : Step.more(acc);
  }

  /**
   * Creates a transducer that takes only the first n elements from the input, passing them to the reducer and ignoring the rest.
   *
   * @param n The number of elements to take from the input
   * @return A new transducer that applies the take logic
   */
  static <A, T> Transducer<A, T, T> take(int n) {
    return new Transducer<>() {

      int state = n;

      public Reducer<A, T> apply(Reducer<A, T> reducer) {
        return (acc, value) -> {
          if (state > 0) {
            state--;
            return reducer.apply(acc, value);
          }
          return Step.done(acc);
        };
      }
    };
  }

  /**
   * Creates a transducer that takes elements from the input as long as they satisfy a given predicate, passing them to the reducer and ignoring the rest.
   *
   * @param matcher The predicate function that determines whether to continue taking elements
   * @return A new transducer that applies the takeWhile logic
   */
  static <A, T> Transducer<A, T, T> takeWhile(Matcher1<? super T> matcher) {
    return reducer -> {
      return (acc, value) -> {
        if (matcher.test(value)) {
          return reducer.apply(acc, value);
        }
        return Step.done(acc);
      };
    };
  }

  /**
   * Creates a transducer that drops the first n elements from the input, passing the rest to the reducer.
   *
   * @param n The number of elements to drop from the input
   * @return A new transducer that applies the drop logic
   */
  static <A, T> Transducer<A, T, T> drop(int n) {
    return new Transducer<>() {

      int state = n;

      public Reducer<A, T> apply(Reducer<A, T> reducer) {
        return (acc, value) -> {
          if (state > 0) {
            state--;
            return Step.more(acc);
          }
          return reducer.apply(acc, value);
        };
      }
    };
  }

  /**
   * Creates a transducer that drops elements from the input as long as they satisfy a given predicate, passing the rest to the reducer.
   *
   * @param matcher The predicate function that determines whether to continue dropping elements
   * @return A new transducer that applies the dropWhile logic
   */
  static <A, T> Transducer<A, T, T> dropWhile(Matcher1<? super T> matcher) {
    return reducer -> {
      return (acc, value) -> {
        if (matcher.test(value)) {
          return Step.more(acc);
        }
        return reducer.apply(acc, value);
      };
    };
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
      var step = r.apply(acc, value);
      if (step instanceof Step.Done(var result)) {
        return result;
      }
      acc = step.value();
    }
    return acc;
  }
}
