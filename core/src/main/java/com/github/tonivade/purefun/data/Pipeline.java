/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.PartialFunction1;
import com.github.tonivade.purefun.core.Tuple2;

/**
 * A pipeline of transducers to be applied to a sequence.
 * The pipeline is immutable and can be reused multiple times.
 *
 * @param <T> the type of input elements
 * @param <U> the type of output elements
 */
public final class Pipeline<T, U> {

  private final Transducer<Object, T, U> transducer;

  private Pipeline(Transducer<Object, T, U> transducer) {
    this.transducer = checkNonNull(transducer, "transducer must not be null");
  }

  /**
   * Collects the results of applying the pipeline to an input collection using the given finisher.
   *
   * @param finisher the finisher to collect the results
   * @param <A> the type of the result produced by the finisher
   * @return the result produced by applying the pipeline and collecting with the finisher
   */
  public <A> A finish(Finisher<A, T, U> finisher) {
    return Pipeline.finish(this, finisher);
  }

  /**
   * Collects the results of applying the pipeline to an input collection using the given finisher.
   *
   * @param pipeline the pipeline to apply
   * @param finisher the finisher to collect the results
   * @param <A> the type of the result produced by the finisher
   * @param <T> the type of input elements processed by the pipeline
   * @param <U> the type of output elements produced by the pipeline
   * @return the result produced by applying the pipeline and collecting with the finisher
   */
  @SuppressWarnings("unchecked")
  public static <A, T, U> A finish(Pipeline<? super T, ? extends U> pipeline, Finisher<A, T, U> finisher) {
    return finisher.apply((Transducer<A, T, U>) pipeline.transducer);
  }

  /**
   * Creates an identity pipeline that does not transform the input elements.
   *
   * @param <T> the type of input and output elements
   * @return an identity pipeline
   */
  public static <T> Pipeline<T, T> identity() {
    return new Pipeline<>(Transducer.identity());
  }

  /**
   * Returns a pipeline that applies the given function to each element of the input sequence.
   *
   * @param f the function to apply to each element
   * @param <V> the type of output elements produced by the function
   * @return a pipeline that applies the function to each element
   */
  public <V> Pipeline<T, V> map(Function1<? super U, ? extends V> f) {
    return chain(Transducer.map(f));
  }

  /**
   * Returns a pipeline that applies the given partial function to each element of the input sequence and filters out elements for which the function is not defined.
   *
   * @param f the partial function to apply to each element
   * @param <V> the type of output elements produced by the function
   * @return a pipeline that applies the partial function and filters out undefined elements
   */
  public <V> Pipeline<T, V> mapFilter(PartialFunction1<? super U, ? extends V> f) {
    return filter(f::isDefinedAt).map(f::apply);
  }

  /**
   * Returns a pipeline that filters the input elements using the given predicate.
   *
   * @param p the predicate to test on each element
   * @return a pipeline that filters the input elements
   */
  public Pipeline<T, U> filter(Matcher1<? super U> p) {
    return chain(Transducer.filter(p));
  }

  /**
   * Returns a pipeline that applies the given function to each element of the input sequence and flattens the results.
   *
   * @param f the function to apply to each element, returning a sequence of results
   * @param <V> the type of output elements produced by the function
   * @return a pipeline that applies the function and flattens the results
   */
  public <V> Pipeline<T, V> flatMap(Function1<? super U, ? extends Sequence<V>> f) {
    return chain(Transducer.flatMap(f));
  }

  /**
   * Returns a pipeline that takes only the first n elements of the input sequence.
   *
   * @param n the number of elements to take
   * @return a pipeline that takes only the first n elements
   */
  public Pipeline<T, U> take(int n) {
    return chain(Transducer.take(n));
  }

  /**
   * Returns a pipeline that drops the first n elements of the input sequence.
   *
   * @param n the number of elements to drop
   * @return a pipeline that drops the first n elements
   */
  public Pipeline<T, U> drop(int n) {
    return chain(Transducer.drop(n));
  }

  /**
   * Returns a pipeline that produces tumbling windows of the input sequence.
   *
   * @param size the size of the tumbling window
   * @return a pipeline that produces sequences of tumbling windows
   */
  public Pipeline<T, Sequence<U>> tumbling(int size) {
    return chain(Transducer.tumbling(size));
  }

  /**
   * Returns a pipeline that produces sliding windows of the input sequence.
   *
   * @param size the size of the sliding window
   * @return a pipeline that produces sequences of sliding windows
   */
  public Pipeline<T, Sequence<U>> sliding(int size) {
    return chain(Transducer.sliding(size));
  }

  /**
   * Returns a pipeline that produces only distinct elements from the input sequence.
   *
   * @return a pipeline that filters out duplicate elements
   */
  public Pipeline<T, U> distinct() {
    return chain(Transducer.distinct());
  }

  /**
   * Returns a pipeline that zips each element of the input sequence with its index.
   *
   * @return a pipeline that produces tuples of (index, element)
   */
  public Pipeline<T, Tuple2<Integer, U>> zipWithIndex() {
    return chain(Transducer.zipWithIndex());
  }

  /**
   * Returns a pipeline that drops elements from the input sequence while the given condition holds.
   *
   * @param condition the condition to test on each element
   * @return a pipeline that drops elements while the condition holds
   */
  public Pipeline<T, U> dropWhile(Matcher1<? super U> condition) {
    return chain(Transducer.dropWhile(condition));
  }

  /**
   * Returns a pipeline that takes elements from the input sequence while the given condition holds.
   *
   * @param condition the condition to test on each element
   * @return a pipeline that takes elements while the condition holds
   */
  public Pipeline<T, U> takeWhile(Matcher1<? super U> condition) {
    return chain(Transducer.takeWhile(condition));
  }

  private <V> Pipeline<T, V> chain(Transducer<Object, U, V> next) {
    return new Pipeline<>(Transducer.chain(transducer, next));
  }
}
