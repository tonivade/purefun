/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
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
  static <A, T, U, V> Transducer<A, T, V> chain(Transducer<A, T, U> t1, Transducer<A, U, V> t2) {
    return reducer -> t1.apply(t2.apply(reducer));
  }

  /**
   * A Transition represents the possible outcomes of processing an input element in a stateful manner.
   * It can emit a single value, emit multiple values, skip the current input, or stop the processing entirely.
   *
   * @param <S> The type of the state maintained by the transition
   * @param <U> The type of the output value(s) produced by the transition
   */
  sealed interface Transition<S, U> {
    /**
     * Represents a transition that emits a single value and updates the state.
     *
     * @param state The new state after processing the input
     * @param value The value to emit as output
     */
    record Emit<S, U>(S state, U value) implements Transition<S, U> {}

    /**
     * Represents a transition that emits multiple values and updates the state.
     *
     * @param state The new state after processing the input
     * @param value The iterable of values to emit as output
     */
    record EmitMany<S, U>(S state, Iterable<U> value) implements Transition<S, U> {}

    /**
     * Represents a transition that skips the current input and updates the state without emitting any value.
     *
     * @param state The new state after processing the input
     */
    record Skip<S, U>(S state) implements Transition<S, U> {}

    /**
     * Represents a transition that stops the processing entirely and updates the state.
     *
     * @param state The new state after processing the input
     */
    record Stop<S, U>(S state) implements Transition<S, U> {}

    /**
     * Creates a transition that emits a single value and updates the state.
     *
     * @param state The new state after processing the input
     * @param value The value to emit as output
     * @return A Transition representing the emission of a single value
     */
    static <S, U> Transition<S, U> emit(S state, U value) {
      return new Emit<>(state, value);
    }

    /**
     * Creates a transition that emits multiple values and updates the state.
     *
     * @param state The new state after processing the input
     * @param value The iterable of values to emit as output
     * @return A Transition representing the emission of multiple values
     */
    static <S, U> Transition<S, U> emitMany(S state, Iterable<U> value) {
      return new EmitMany<>(state, value);
    }

    /**
     * Creates a transition that skips the current input and updates the state without emitting any value.
     *
     * @param state The new state after processing the input
     * @return A Transition representing the skipping of the current input
     */
    static <S, U> Transition<S, U> skip(S state) {
      return new Skip<>(state);
    }

    /**
     * Creates a transition that stops the processing entirely and updates the state.
     *
     * @param state The new state after processing the input
     * @return A Transition representing the stopping of the processing
     */
    static <S, U> Transition<S, U> stop(S state) {
      return new Stop<>(state);
    }
  }

  /**
   * Creates a stateful mapping transducer that maintains an internal state and produces output based on the current state and input value.
   * The state is initialized using the provided init function, and the step function defines how to transition from one state to another and what output to produce for each input value.
   * The step function can emit a single value, emit multiple values, skip the current input, or stop the processing entirely based on the current state and input value.
   * The state is updated after processing each input value, allowing for dynamic behavior that can adapt to the sequence of inputs.
   *
   * @param init A producer function that initializes the state before processing any input elements
   * @param step A function that takes the current state and an input value, and returns
   *             a Transition that defines how to update the state and what output to produce for the given input value
   * @param <A> The type of the accumulator (e.g., List, Set, etc.)
   * @param <T> The type of the input elements
   * @param <S> The type of the internal state maintained by the transducer
   * @param <U> The type of the output elements produced by the transducer
   * @return A new transducer that applies the stateful mapping logic defined by the init and step functions
   */
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
   * Creates a transducer that simply passes input elements through to the reducer without any transformation.
   *
   * @return A new transducer that acts as an identity function, passing input elements directly to the reducer
   */
  static <A, T> Transducer<A, T, T> identity() {
    return reducer -> reducer;
  }

  /**
   * Creates a transducer that applies a mapping function to each input element before passing it to the reducer.
   *
   * @param mapper The mapping function to apply to each input element
   * @return A new transducer that applies the mapping function
   */
  static <A, T, U> Transducer<A, T, U> map(Function1<? super T, ? extends U> mapper) {
    return statefulMap(Unit::unit, (s, t) -> Transition.emit(s, mapper.apply(t)));
  }

  /**
   * Creates a transducer that applies a flat-mapping function to each input element, allowing for multiple output elements per input.
   *
   * @param mapper The flat-mapping function that returns a sequence of output elements for each input element
   * @return A new transducer that applies the flat-mapping function
   */
  static <A, T, U> Transducer<A, T, U> flatMap(Function1<? super T, ? extends Sequence<U>> mapper) {
    return statefulMap(Unit::unit, (s, t) -> Transition.emitMany(s, mapper.apply(t)));
  }

  /**
   * Creates a transducer that filters input elements based on a predicate, only passing those that satisfy the predicate to the reducer.
   *
   * @param matcher The predicate function that determines whether an input element should be included
   * @return A new transducer that applies the filtering logic
   */
  static <A, T> Transducer<A, T, T> filter(Matcher1<? super T> matcher) {
    return statefulMap(Unit::unit, (s, t) -> matcher.test(t) ? Transition.emit(s, t) : Transition.skip(s));
  }

  /**
   * Creates a transducer that takes only the first n elements from the input, passing them to the reducer and ignoring the rest.
   *
   * @param n The number of elements to take from the input
   * @return A new transducer that applies the take logic
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
   * Creates a transducer that takes elements from the input as long as they satisfy a given predicate, passing them to the reducer and ignoring the rest.
   *
   * @param matcher The predicate function that determines whether to continue taking elements
   * @return A new transducer that applies the takeWhile logic
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
   * Creates a transducer that drops the first n elements from the input, passing the rest to the reducer.
   *
   * @param n The number of elements to drop from the input
   * @return A new transducer that applies the drop logic
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
   * Creates a transducer that drops elements from the input as long as they satisfy a given predicate, passing the rest to the reducer.
   *
   * @param matcher The predicate function that determines whether to continue dropping elements
   * @return A new transducer that applies the dropWhile logic
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
   * Creates a transducer that ensures only distinct elements are passed to the reducer, filtering out duplicates based on their natural equality.
   * This transducer maintains a set of seen elements and only allows unique elements to be processed by the reducer.
   * Note that this transducer may have performance implications for large inputs due to the need to maintain a set of seen elements.
   * It is recommended to use this transducer when the input size is manageable and the uniqueness of elements is a requirement.
   *
   * @return A new transducer that filters out duplicate elements, allowing only distinct elements to be processed by the reducer
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
   * Creates a transducer that zips each input element with its corresponding index, passing a tuple of (index, element) to the reducer.
   * The index starts at 0 and increments for each element processed.
   *
   * @return A new transducer that applies the zipWithIndex logic
   */
  static <A, T> Transducer<A, T, Tuple2<Integer, T>> zipWithIndex() {
    return statefulMap(() -> 0, (index, value) -> Transition.emit(index + 1, Tuple.of(index, value)));
  }

  /**
   * Creates a transducer that groups input elements into fixed-size windows and passes each window as a sequence to the reducer.
   * The last window may contain fewer than the specified size if there are not enough remaining elements.
   *
   * @param size The size of each fixed window
   * @return A new transducer that applies the fixed window logic
   */
  static <A, T> Transducer<A, T, Sequence<T>> windowFixed(int size) {
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
   * Creates a transducer that groups input elements into fixed-size sliding windows and passes each window as a sequence to the reducer.
   * The windows overlap, meaning that each window includes the last (size - 1) elements of the previous window.
   * The first few windows will contain fewer than the specified size until enough elements have been processed.
   *
   * @param size The size of each sliding window
   * @return A new transducer that applies the sliding window logic
   */
  static <A, T> Transducer<A, T, Sequence<T>> windowSliding(int size) {
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
   * Creates a transducer that performs a cumulative reduction (scan) on the input elements, producing a running total of the reduction.
   * The initial value for the reduction is provided, and the reducer function defines how to combine the current accumulated value with each input element to produce the next accumulated value.
   * The transducer emits the accumulated value after processing each input element, allowing you to track the progression of the reduction over time.
   *
   * @param initial The initial value for the cumulative reduction
   * @param reducer A function that takes the current accumulated value and an input element, and returns the next accumulated value
   * @return A new transducer that applies the scan logic, emitting the accumulated value after processing each input element
   */
  static <A, T, R> Transducer<A, T, R> scan(R initial, Function2<? super R, ? super T, ? extends R> reducer) {
    return statefulMap(() -> Tuple.of(initial, false), (state, value) -> {
      R next = reducer.apply(state.get1(), value);
      if (state.get2()) {
        return Transition.emit(Tuple.of(next, true), next);
      }
      return Transition.emitMany(Tuple.of(next, true), List.of(state.get1(), next));
    });
  }
}
