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
    return xf -> Transducer.run(xf, (acc, e) -> Step.more(acc.append(e)), ImmutableArray.<R>empty(), input);
  }

  static <E, R> Finisher<ImmutableList<R>, E, R> toImmutableList(Iterable<E> input) {
    return sf -> Transducer.run(sf, (acc, e) -> Step.more(acc.append(e)), ImmutableList.<R>empty(), input);
  }

  static <E, R> Finisher<ImmutableSet<R>, E, R> toImmutableSet(Iterable<E> input) {
    return sf -> Transducer.run(sf, (acc, e) -> Step.more(acc.append(e)), ImmutableSet.<R>empty(), input);
  }

  static <E, R> Finisher<ImmutableTree<R>, E, R> toImmutableTree(
      Comparator<? super R> comparator, Iterable<E> input) {
    return sf -> Transducer.run(sf, (acc, e) -> Step.more(acc.append(e)), ImmutableTree.<R>empty(comparator), input);
  }
}
