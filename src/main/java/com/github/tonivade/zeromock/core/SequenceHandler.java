/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

@FunctionalInterface
public interface SequenceHandler<T, R> extends Function1<T, Sequence<R>> {
  
  default <V> SequenceHandler<T, V> map(Function1<R, V> mapper) {
    return value -> apply(value).map(mapper::apply);
  }
  
  default <V> SequenceHandler<T, V> flatMap(SequenceHandler<R, V> mapper) {
    return value -> apply(value).flatMap(mapper::apply);
  }
  
  default <V> SequenceHandler<T, V> flatten() {
    return value -> apply(value).flatten();
  }
  
  default SequenceHandler<T, R> filter(Matcher<R> matcher) {
    return value -> apply(value).filter(matcher::match);
  }

  default StreamHandler<T, R> toStreamHandler() {
    return value -> apply(value).stream();
  }

  static <T> SequenceHandler<Sequence<T>, T> identity() {
    return Function1.<Sequence<T>>identity()::apply;
  }
}
