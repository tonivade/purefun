/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.PartialFunction1;
import com.github.tonivade.purefun.core.Tuple2;

/**
 * A pipeline with an input, used to apply the pipeline to the input when finished
 *
 * @param <T> the type of the input elements
 * @param <U> the type of the output elements
 */
public final class PipelineWithInput<T, U> {

  private final Pipeline<T, U> pipeline;
  private final Iterable<T> input;

  public PipelineWithInput(Pipeline<T, U> pipeline, Iterable<T> input) {
    this.pipeline = checkNonNull(pipeline, "pipeline must not be null");
    this.input = checkNonNull(input, "input must not be null");
  }

  public <A> A finish(Function1<Iterable<T>, Finisher<A, T, U>> finisher) {
    return pipeline.finish(finisher.apply(input));
  }

  public void forEach(Consumer1<? super U> action) {
    pipeline.forEach(input, action);
  }

  public <V> PipelineWithInput<T, V> map(Function1<? super U, ? extends V> f) {
    return new PipelineWithInput<>(pipeline.map(f), input);
  }

  public <V> PipelineWithInput<T, V> mapFilter(PartialFunction1<? super U, ? extends V> f) {
    return new PipelineWithInput<>(pipeline.mapFilter(f), input);
  }

  public PipelineWithInput<T, U> filter(Matcher1<? super U> p) {
    return new PipelineWithInput<>(pipeline.filter(p), input);
  }

  public PipelineWithInput<T, U> filterNot(Matcher1<? super U> p) {
    return new PipelineWithInput<>(pipeline.filterNot(p), input);
  }

  public <V> PipelineWithInput<T, V> flatMap(Function1<? super U, ? extends Sequence<V>> f) {
    return new PipelineWithInput<>(pipeline.flatMap(f), input);
  }

  public PipelineWithInput<T, U> take(int n) {
    return new PipelineWithInput<>(pipeline.take(n), input);
  }

  public PipelineWithInput<T, U> drop(int n) {
    return new PipelineWithInput<>(pipeline.drop(n), input);
  }

  public PipelineWithInput<T, Sequence<U>> tumbling(int size) {
    return new PipelineWithInput<>(pipeline.tumbling(size), input);
  }

  public PipelineWithInput<T, Sequence<U>> sliding(int size) {
    return new PipelineWithInput<>(pipeline.sliding(size), input);
  }

  public PipelineWithInput<T, U> distinct() {
    return new PipelineWithInput<>(pipeline.distinct(), input);
  }

  public PipelineWithInput<T, Tuple2<Integer, U>> zipWithIndex() {
    return new PipelineWithInput<>(pipeline.zipWithIndex(), input);
  }

  public PipelineWithInput<T, U> dropWhile(Matcher1<? super U> condition) {
    return new PipelineWithInput<>(pipeline.dropWhile(condition), input);
  }

  public PipelineWithInput<T, U> takeWhile(Matcher1<? super U> condition) {
    return new PipelineWithInput<>(pipeline.takeWhile(condition), input);
  }

  public <V> PipelineWithInput<T, V> scan(V init, Function2<? super V, ? super U, ? extends V> f) {
    return new PipelineWithInput<>(pipeline.scan(init, f), input);
  }
}
