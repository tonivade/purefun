/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.data.Reducer.Step;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Pipeline is a higher-order function that takes a reducer and returns a new reducer.
 * It allows you to compose transformations on data without creating intermediate collections.
 *
 * @param <A> The type of the accumulator (e.g., List, Set, etc.)
 * @param <T> The type of the input elements
 * @param <U> The type of the output elements after transformation
 */
public interface Pipeline<A, T, U> {

  /**
   * Applies this pipeline to a given reducer, returning a new reducer that incorporates the transformation logic.
   *
   * @param reducer The original reducer to which the pipeline will be applied
   * @return A new reducer
   */
  Reducer<A, T> apply(Reducer<A, U> reducer);

  @SuppressWarnings("unchecked")
  default <R extends Iterable<U>> Pipeline<R, T, U> narrowK() {
    return (Pipeline<R, T, U>) this;
  }

  /**
   * Composes two pipelines into a single pipeline that applies both transformations in sequence.
   *
   * @param t1 The first pipeline to apply
   * @param t2 The second pipeline to apply after the first one
   * @return A new pipeline that represents the composition of t1 and t2
   */
  static <A, T, U, V> Pipeline<A, T, V> chain(Pipeline<A, T, U> t1, Pipeline<A, U, V> t2) {
    return reducer -> t1.apply(t2.apply(reducer));
  }

  /**
   * Composes three pipelines into a single pipeline that applies all three transformations in sequence.
   *
   * @param t1 The first pipeline to apply
   * @param t2 The second pipeline to apply after the first one
   * @param t3 The third pipeline to apply after the second one
   * @return A new pipeline that represents the composition of t1, t2, and t3
   */
  static <A, T, U, V, W> Pipeline<A, T, W> chain(
      Pipeline<A, T, U> t1, Pipeline<A, U, V> t2, Pipeline<A, V, W> t3) {
    return reducer -> t1.apply(t2.apply(t3.apply(reducer)));
  }

  /**
   * Creates a pipeline that simply passes input elements through to the reducer without any transformation.
   *
   * @return A new pipeline that acts as an identity function, passing input elements directly to the reducer
   */
  static <A, T> Pipeline<A, T, T> identity() {
    return reducer -> reducer;
  }

  /**
   * Creates a pipeline that applies a mapping function to each input element before passing it to the reducer.
   *
   * @param mapper The mapping function to apply to each input element
   * @return A new pipeline that applies the mapping function
   */
  static <A, T, U> Pipeline<A, T, U> map(Function1<? super T, ? extends U> mapper) {
    return reducer ->
        (acc, value) -> reducer.apply(acc, mapper.apply(value));
  }

  /**
   * Creates a pipeline that applies a flat-mapping function to each input element, allowing for multiple output elements per input.
   *
   * @param mapper The flat-mapping function that returns a sequence of output elements for each input element
   * @return A new pipeline that applies the flat-mapping function
   */
  static <A, T, U> Pipeline<A, T, U> flatMap(Function1<? super T, ? extends Sequence<U>> mapper) {
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
   * Creates a pipeline that filters input elements based on a predicate, only passing those that satisfy the predicate to the reducer.
   *
   * @param matcher The predicate function that determines whether an input element should be included
   * @return A new pipeline that applies the filtering logic
   */
  static <A, T> Pipeline<A, T, T> filter(Matcher1<? super T> matcher) {
    return reducer ->
        (acc, value) -> matcher.test(value)
            ? reducer.apply(acc, value)
            : Step.more(acc);
  }

  /**
   * Creates a pipeline that takes only the first n elements from the input, passing them to the reducer and ignoring the rest.
   *
   * @param n The number of elements to take from the input
   * @return A new pipeline that applies the take logic
   */
  static <A, T> Pipeline<A, T, T> take(int n) {
    return new Pipeline<>() {

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
   * Creates a pipeline that takes elements from the input as long as they satisfy a given predicate, passing them to the reducer and ignoring the rest.
   *
   * @param matcher The predicate function that determines whether to continue taking elements
   * @return A new pipeline that applies the takeWhile logic
   */
  static <A, T> Pipeline<A, T, T> takeWhile(Matcher1<? super T> matcher) {
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
   * Creates a pipeline that drops the first n elements from the input, passing the rest to the reducer.
   *
   * @param n The number of elements to drop from the input
   * @return A new pipeline that applies the drop logic
   */
  static <A, T> Pipeline<A, T, T> drop(int n) {
    return new Pipeline<>() {

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
   * Creates a pipeline that drops elements from the input as long as they satisfy a given predicate, passing the rest to the reducer.
   *
   * @param matcher The predicate function that determines whether to continue dropping elements
   * @return A new pipeline that applies the dropWhile logic
   */
  static <A, T> Pipeline<A, T, T> dropWhile(Matcher1<? super T> matcher) {
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
   * Creates a pipeline that ensures only distinct elements are passed to the reducer, filtering out duplicates based on their natural equality.
   * This pipeline maintains a set of seen elements and only allows unique elements to be processed by the reducer.
   * Note that this pipeline may have performance implications for large inputs due to the need to maintain a set of seen elements.
   * It is recommended to use this pipeline when the input size is manageable and the uniqueness of elements is a requirement.
   *
   * @return A new pipeline that filters out duplicate elements, allowing only distinct elements to be processed by the reducer
   */
  static <A, T> Pipeline<A, T, T> distinct() {
    return reducer -> {
      var seen = new HashSet<T>();

      return (acc, value) -> {
        if (seen.contains(value)) {
          return Step.more(acc);
        }
        seen.add(value);
        return reducer.apply(acc, value);
      };
    };
  }

  /**
   * Creates a pipeline that zips each input element with its corresponding index, passing a tuple of (index, element) to the reducer.
   * The index starts at 0 and increments for each element processed.
   *
   * @return A new pipeline that applies the zipWithIndex logic
   */
  static <A, T> Pipeline<A, T, Tuple2<Integer, T>> zipWithIndex() {
    return new Pipeline<>() {

      int index = 0;

      @Override
      public Reducer<A, T> apply(Reducer<A, Tuple2<Integer, T>> reducer) {
        return (acc, value) -> reducer.apply(acc, Tuple.of(index++, value));
      }
    };
  }

  /**
   * Creates a pipeline that groups input elements into fixed-size windows (tumbling windows) and passes each window as a sequence to the reducer.
   * The last window may contain fewer than the specified size if there are not enough remaining elements.
   *
   * @param size The size of each tumbling window
   * @return A new pipeline that applies the tumbling window logic
   */
  static <A, T> Pipeline<A, T, Sequence<T>> tumbling(int size) {
    return reducer -> {
      var window = new ArrayList<T>(size);

      return (acc, value) -> {
        window.add(value);
        if (window.size() == size) {
          var step = reducer.apply(acc, ImmutableList.from(window));
          window.clear();
          return step;
        }
        return Step.more(acc);
      };
    };
  }

  /**
   * Creates a pipeline that groups input elements into fixed-size windows (sliding windows) and passes each window as a sequence to the reducer.
   * The windows overlap, meaning that each window includes the last (size - 1) elements of the previous window.
   * The first few windows will contain fewer than the specified size until enough elements have been processed.
   *
   * @param size The size of each sliding window
   * @return A new pipeline that applies the sliding window logic
   */
  static <A, T> Pipeline<A, T, Sequence<T>> sliding(int size) {
    return reducer -> {
      var window = new ArrayDeque<T>(size);

      return (acc, value) -> {
        window.addLast(value);
        if (window.size() < size) {
          return Step.more(acc);
        }
        if (window.size() > size) {
          window.removeFirst();
        }
        return reducer.apply(acc, ImmutableList.from(window));
      };
    };
  }

  /**
   * Applies the pipeline to the input data using the provided pipeline and reducer, starting with an initial accumulator value.
   *
   * @param pipeline The pipeline to apply to the input data
   * @param reducer The reducer that will be used to accumulate the results
   * @param init The initial value of the accumulator
   * @param input The input data to be processed
   * @return The final accumulated result after processing
   */
  static <A extends Iterable<U>, T, U> A run(
      Pipeline<A, T, U> pipeline, Reducer<A, U> reducer, A init, Iterable<T> input) {
    var r = pipeline.apply(reducer);
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
