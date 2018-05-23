/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.stream.Collector;
import java.util.stream.Stream;

@FunctionalInterface
public interface StreamHandler<T, R> extends Handler1<T, Stream<R>> {
  
  default <V> StreamHandler<T, V> map(Handler1<R, V> handler) {
    return value -> handle(value).map(handler::handle);
  }
  
  default <V> StreamHandler<T, V> flatMap(Handler1<R, Stream<V>> handler) {
    return value -> handle(value).flatMap(handler::handle);
  }
  
  default StreamHandler<T, R> filter(Matcher<R> matcher) {
    return value -> handle(value).filter(matcher::match);
  }
  
  default <A, V> Handler1<T, V> collect(Collector<R, A, V> collector) {
    return value -> handle(value).collect(collector);
  }
}
