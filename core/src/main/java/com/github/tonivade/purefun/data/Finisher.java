/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import java.util.Comparator;

import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.data.Reducer.Step;

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
  static <A extends Iterable<R>, E, R> Finisher<A, E, R> of(Iterable<E> input, Producer<A> init, Function2<A, R, A> append) {
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
  static <E, R> Finisher<ImmutableArray<R>, E, R> toImmutableArray(Iterable<E> input) {
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
  static <E, R> Finisher<ImmutableList<R>, E, R> toImmutableList(Iterable<E> input) {
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
  static <E, R> Finisher<ImmutableSet<R>, E, R> toImmutableSet(Iterable<E> input) {
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
      Comparator<? super R> comparator, Iterable<E> input) {
    return of(input, () -> ImmutableTree.<R>empty(comparator), ImmutableTree::append);
  }

  private static <A extends Iterable<U>, T, U> A run(A init, Iterable<T> input, Reducer<A, T> reducer) {
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
