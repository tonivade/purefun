/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.function.Function.identity;

import java.util.stream.Collector;
import java.util.stream.Stream;

@FunctionalInterface
public interface StreamHandler<T, R> extends Function1<T, Stream<R>> {

  @Override
  default <V> StreamHandler<V, R> compose(Function1<V, T> before) {
    return value -> apply(before.apply(value));
  }
  
  default <V> StreamHandler<T, V> map(Function1<R, V> mapper) {
    return value -> apply(value).map(mapper::apply);
  }
  
  default <V> StreamHandler<T, V> flatMap(StreamHandler<R, V> mapper) {
    return value -> apply(value).flatMap(mapper::apply);
  }
  
  @SuppressWarnings("unchecked")
  default <V> StreamHandler<T, V> flatten() {
    return value -> ((Stream<Stream<V>>) apply(value)).flatMap(identity());
  }
  
  default StreamHandler<T, R> filter(Matcher<R> matcher) {
    return value -> apply(value).filter(matcher::match);
  }
  
  default SequenceHandler<T, R> toSequenceHandler() {
    return value -> ImmutableList.from(apply(value));
  }
  
  default <A, V> Function1<T, V> collect(Collector<R, A, V> collector) {
    return value -> apply(value).collect(collector);
  }
  
  static <T, R> StreamHandler<T, R> of(Function1<T, Stream<R>> reference) {
    return reference::apply;
  }
}
