/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Unit.unit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.data.Reducer.Step;

/**
 * Pipeline is a higher-order function that takes a reducer and returns a new reducer.
 * It allows you to compose transformations on data without creating intermediate collections.
 *
 * @param <A> The type of the accumulator (e.g., List, Set, etc.)
 * @param <T> The type of the input elements
 * @param <U> The type of the output elements after transformation
 */
public interface Transducer<A, T, U> {

  /**
   * Applies this pipeline to a given reducer, returning a new reducer that incorporates the transformation logic.
   *
   * @param reducer The original reducer to which the pipeline will be applied
   * @return A new reducer
   */
  Reducer<A, T> apply(Reducer<A, U> reducer);

  @SuppressWarnings("unchecked")
  default <R extends Iterable<U>> Transducer<R, T, U> fix() {
    return (Transducer<R, T, U>) this;
  }

  /**
   * Composes two transducers into a single transducer that applies both transformations in sequence.
   *
   * @param t1 The first transducer to apply
   * @param t2 The second transducer to apply after the first one
   * @param <A> The type of the accumulator (e.g., List, Set, etc.)
   * @param <T> The type of the input elements
   * @param <U> The type of the intermediate output elements after the first transformation
   * @param <V> The type of the final output elements after the second transformation
   * @return A new transducer that represents the composition of t1 and t2
   */
  static <A, T, U, V> Transducer<A, T, V> chain(
      Transducer<A, T, U> t1, Transducer<A, U, V> t2) {
    return reducer -> t1.apply(t2.apply(reducer));
  }

  sealed interface Transition<S, U> {
    record Emit<S, U>(S state, U value) implements Transition<S, U> {}
    record EmitMany<S, U>(S state, Iterable<U> value) implements Transition<S, U> {}
    record Skip<S, U>(S state) implements Transition<S, U> {}
    record Stop<S, U>(S state) implements Transition<S, U> {}

    static <S, U> Transition<S, U> emit(S state, U value) {
      return new Emit<>(state, value);
    }

    static <S, U> Transition<S, U> emitMany(S state, Iterable<U> value) {
      return new EmitMany<>(state, value);
    }

    static <S, U> Transition<S, U> skip(S state) {
      return new Skip<>(state);
    }

    static <S, U> Transition<S, U> stop(S state) {
      return new Stop<>(state);
    }
  }

  static <A, T, S, U> Transducer<A, T, U> statefulMap(
      Producer<S> init, Function2<? super S, ? super T, ? extends Transition<S, U>> step) {
    return new Transducer<>() {

      S state = init.get();

      @Override
      public Reducer<A, T> apply(Reducer<A, U> reducer) {

        return (acc, value) -> {
          var t = step.apply(state, value);

          return switch (t) {
            case Transition.Emit<S, U>(var nextState, var nextValue) -> {
              state = nextState;
              yield reducer.apply(acc, nextValue);
            }
            case Transition.EmitMany<S, U>(var nextState, var nextValues) -> {
              state = nextState;
              var result = acc;
              for (var u : nextValues) {
                var inner = reducer.apply(result, u);
                if (inner instanceof Step.Done) {
                  yield inner;
                }
                result = inner.value();
              }
              yield Step.more(result);
            }
            case Transition.Skip<S, U>(var nextState) -> {
              state = nextState;
              yield Step.more(acc);
            }
            case Transition.Stop<S, U>(var nextState) -> {
              state = nextState;
              yield Step.done(acc);
            }
          };
        };
      }
    };
  }

  /**
   * Creates a pipeline that simply passes input elements through to the reducer without any transformation.
   *
   * @return A new pipeline that acts as an identity function, passing input elements directly to the reducer
   */
  static <A, T> Transducer<A, T, T> identity() {
    return reducer -> reducer;
  }

  /**
   * Creates a pipeline that applies a mapping function to each input element before passing it to the reducer.
   *
   * @param mapper The mapping function to apply to each input element
   * @return A new pipeline that applies the mapping function
   */
  static <A, T, U> Transducer<A, T, U> map(Function1<? super T, ? extends U> mapper) {
    return statefulMap(() -> unit(), (s, t) -> Transition.emit(s, mapper.apply(t)));
  }

  /**
   * Creates a pipeline that applies a flat-mapping function to each input element, allowing for multiple output elements per input.
   *
   * @param mapper The flat-mapping function that returns a sequence of output elements for each input element
   * @return A new pipeline that applies the flat-mapping function
   */
  static <A, T, U> Transducer<A, T, U> flatMap(Function1<? super T, ? extends Sequence<U>> mapper) {
    return statefulMap(() -> unit(), (s, t) -> Transition.emitMany(s, mapper.apply(t)));
  }

  /**
   * Creates a pipeline that filters input elements based on a predicate, only passing those that satisfy the predicate to the reducer.
   *
   * @param matcher The predicate function that determines whether an input element should be included
   * @return A new pipeline that applies the filtering logic
   */
  static <A, T> Transducer<A, T, T> filter(Matcher1<? super T> matcher) {
    return statefulMap(() -> unit(), (s, t) -> {
      if (matcher.test(t)) {
        return Transition.emit(s, t);
      }
      return Transition.skip(s);
    });
  }

  /**
   * Creates a pipeline that takes only the first n elements from the input, passing them to the reducer and ignoring the rest.
   *
   * @param n The number of elements to take from the input
   * @return A new pipeline that applies the take logic
   */
  static <A, T> Transducer<A, T, T> take(int n) {
    return statefulMap(() -> n, (state, value) -> {
      if (state > 0) {
        return Transition.emit(state - 1, value);
      }
      return Transition.stop(state);
    });
  }

  /**
   * Creates a pipeline that takes elements from the input as long as they satisfy a given predicate, passing them to the reducer and ignoring the rest.
   *
   * @param matcher The predicate function that determines whether to continue taking elements
   * @return A new pipeline that applies the takeWhile logic
   */
  static <A, T> Transducer<A, T, T> takeWhile(Matcher1<? super T> matcher) {
    return statefulMap(() -> true, (state, value) -> {
      if (state && matcher.test(value)) {
        return Transition.emit(true, value);
      }
      return Transition.stop(false);
    });
  }

  /**
   * Creates a pipeline that drops the first n elements from the input, passing the rest to the reducer.
   *
   * @param n The number of elements to drop from the input
   * @return A new pipeline that applies the drop logic
   */
  static <A, T> Transducer<A, T, T> drop(int n) {
    return statefulMap(() -> n, (state, value) -> {
      if (state > 0) {
        return Transition.skip(state - 1);
      }
      return Transition.emit(state, value);
    });
  }

  /**
   * Creates a pipeline that drops elements from the input as long as they satisfy a given predicate, passing the rest to the reducer.
   *
   * @param matcher The predicate function that determines whether to continue dropping elements
   * @return A new pipeline that applies the dropWhile logic
   */
  static <A, T> Transducer<A, T, T> dropWhile(Matcher1<? super T> matcher) {
    return statefulMap(() -> true, (state, value) -> {
      if (state && matcher.test(value)) {
        return Transition.skip(true);
      }
      return Transition.emit(false, value);
    });
  }

  /**
   * Creates a pipeline that ensures only distinct elements are passed to the reducer, filtering out duplicates based on their natural equality.
   * This pipeline maintains a set of seen elements and only allows unique elements to be processed by the reducer.
   * Note that this pipeline may have performance implications for large inputs due to the need to maintain a set of seen elements.
   * It is recommended to use this pipeline when the input size is manageable and the uniqueness of elements is a requirement.
   *
   * @return A new pipeline that filters out duplicate elements, allowing only distinct elements to be processed by the reducer
   */
  static <A, T> Transducer<A, T, T> distinct() {
    return statefulMap(() -> new HashSet<T>(), (seen, value) -> {
      if (seen.add(value)) {
        return Transition.emit(seen, value);
      }
      return Transition.skip(seen);
    });
  }

  /**
   * Creates a pipeline that zips each input element with its corresponding index, passing a tuple of (index, element) to the reducer.
   * The index starts at 0 and increments for each element processed.
   *
   * @return A new pipeline that applies the zipWithIndex logic
   */
  static <A, T> Transducer<A, T, Tuple2<Integer, T>> zipWithIndex() {
    return statefulMap(() -> 0, (index, value) -> Transition.emit(index + 1, Tuple.of(index, value)));
  }

  /**
   * Creates a pipeline that groups input elements into fixed-size windows (tumbling windows) and passes each window as a sequence to the reducer.
   * The last window may contain fewer than the specified size if there are not enough remaining elements.
   *
   * @param size The size of each tumbling window
   * @return A new pipeline that applies the tumbling window logic
   */
  static <A, T> Transducer<A, T, Sequence<T>> tumbling(int size) {
    return Transducer.<A, T, ArrayList<T>, Sequence<T>>statefulMap(() -> new ArrayList<T>(size), (window, value) -> {
      window.add(value);
      if (window.size() == size) {
        var result = ImmutableList.from(window);
        window.clear();
        return Transition.emit(window, result);
      }
      return Transition.skip(window);
    });
  }

  /**
   * Creates a pipeline that groups input elements into fixed-size windows (sliding windows) and passes each window as a sequence to the reducer.
   * The windows overlap, meaning that each window includes the last (size - 1) elements of the previous window.
   * The first few windows will contain fewer than the specified size until enough elements have been processed.
   *
   * @param size The size of each sliding window
   * @return A new pipeline that applies the sliding window logic
   */
  static <A, T> Transducer<A, T, Sequence<T>> sliding(int size) {
    return statefulMap(() -> new ArrayDeque<T>(size), (window, value) -> {
      window.addLast(value);
      if (window.size() > size) {
        window.removeFirst();
      }
      if (window.size() == size) {
        return Transition.emit(window, ImmutableList.from(window));
      }
      return Transition.skip(window);
    });
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
      Transducer<A, T, U> pipeline, Reducer<A, U> reducer, A init, Iterable<T> input) {
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
