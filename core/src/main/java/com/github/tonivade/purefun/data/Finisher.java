/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import java.util.Comparator;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Reducer.Step;
import com.github.tonivade.purefun.type.Option;

/**
 * A Finisher is a function that takes a Transducer and produces a result of type A.
 * It is used to run a Transducer on an input collection and produce a final result.
 *
 * @param <A> the type of the result produced by the Finisher
 * @param <T> the type of input elements processed by the Transducer
 * @param <U> the type of output elements produced by the Transducer
 */
@FunctionalInterface
public interface Finisher<A, T, U> {

  /**
   * Applies the given Transducer to produce a result of type A.
   *
   * @param transducer the Transducer to apply
   * @return the result produced by applying the Transducer
   */
  A apply(Transducer<A, T, U> transducer);

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and performs the given action on each output element.
   *
   * @param input the input collection to process
   * @param action the action to perform on each output element
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements produced by the Transducer
   * @return a Finisher that runs the given Transducer on the input collection and performs the given action on each output element
   */
  static <E, R> Finisher<Unit, E, R> forEach(Iterable<? extends E> input, Consumer1<? super R> action) {
    return of(input, Unit::unit, (acc, e) -> {
      action.accept(e);
      return acc;
    });
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces a String by joining the string representations of the output elements with the given separator.
   *
   * @param input the input collection to process
   * @param separator the separator to use between the string representations of the output elements
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements produced by the Transducer
   * @return a Finisher that runs the given Transducer on the input collection and produces a String by joining the string representations of the output elements with the given separator
   */
  static <E, R> Finisher<String, E, R> join(Iterable<? extends E> input, String separator) {
    return of(input, String::new, (acc, e) -> acc.isEmpty() ? e.toString() : acc + separator + e);
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an Option containing the first output element, or None if there are no output elements.
   *
   * @param input the input collection to process
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements produced by the Transducer
   * @return a Finisher that runs the given Transducer on the input collection and produces an Option containing the first output element, or None if there are no output elements
   */
  static <E, R> Finisher<Option<R>, E, R> findFirst(Iterable<? extends E> input) {
    return xf -> run(Option.none(), input, xf.apply((acc, e) -> Step.done(Option.some(e))));
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces a result of type A.
   *
   * @param input the input collection to process
   * @param init a Producer that provides the initial value of type A
   * @param append a Function2 that takes the current accumulated value of type A and an element of type E,
   *               and produces a new accumulated value of type A
   * @param <A> the type of the result produced by the Finisher
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements produced by the Transducer
   * @return a Finisher that runs the given Transducer on the input collection and produces a result of type A
   */
  static <A, E, R> Finisher<A, E, R> of(
      Iterable<? extends E> input, Producer<A> init, Function2<? super A, ? super R, ? extends A> append) {
    return xf -> run(init.get(), input, xf.apply((acc, e) -> Step.more(append.apply(acc, e))));
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableArray of type R.
   *
   * @param input the input collection to process
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableArray of type R
   */
  static <E, R> Finisher<ImmutableArray<R>, E, R> toImmutableArray(Iterable<? extends E> input) {
    return of(input, ImmutableArray::empty, ImmutableArray::append);
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableList of type R.
   *
   * @param input the input collection to process
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableList of type R
   */
  static <E, R> Finisher<ImmutableList<R>, E, R> toImmutableList(Iterable<? extends E> input) {
    return of(input, ImmutableList::empty, ImmutableList::append);
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableSet of type R.
   *
   * @param input the input collection to process
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableSet of type R
   */
  static <E, R> Finisher<ImmutableSet<R>, E, R> toImmutableSet(Iterable<? extends E> input) {
    return of(input, ImmutableSet::empty, ImmutableSet::append);
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableTree of type R.
   *
   * @param comparator the Comparator to use for ordering the elements in the ImmutableTree
   * @param input the input collection to process
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableTree of type R
   */
  static <E, R> Finisher<ImmutableTree<R>, E, R> toImmutableTree(
      Comparator<? super R> comparator, Iterable<? extends E> input) {
    return of(input, () -> ImmutableTree.empty(comparator), ImmutableTree::append);
  }

  private static <A, T> A run(A init, Iterable<? extends T> input, Reducer<A, T> reducer) {
    var acc = init;
    for (var value : input) {
      var step = reducer.apply(acc, value);
      if (step instanceof Step.Done(var result)) {
        return result;
      }
      acc = step.value();
    }
    return acc;
  }
}
