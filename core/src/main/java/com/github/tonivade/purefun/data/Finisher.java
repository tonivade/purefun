/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import java.util.Comparator;

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

  static <E, R> Finisher<ImmutableArray<R>, E, R> toImmutableArray(Iterable<E> input) {
    return xf -> run(ImmutableArray.<R>empty(), input, xf.apply((acc, e) -> Step.more(acc.append(e))));
  }

  static <E, R> Finisher<ImmutableList<R>, E, R> toImmutableList(Iterable<E> input) {
    return sf -> run(ImmutableList.<R>empty(), input, sf.apply((acc, e) -> Step.more(acc.append(e))));
  }

  static <E, R> Finisher<ImmutableSet<R>, E, R> toImmutableSet(Iterable<E> input) {
    return sf -> run(ImmutableSet.<R>empty(), input, sf.apply((acc, e) -> Step.more(acc.append(e))));
  }

  static <E, R> Finisher<ImmutableTree<R>, E, R> toImmutableTree(
      Comparator<? super R> comparator, Iterable<E> input) {
    return sf -> run(ImmutableTree.<R>empty(comparator), input, sf.apply((acc, e) -> Step.more(acc.append(e))));
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
