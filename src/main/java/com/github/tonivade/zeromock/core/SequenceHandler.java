/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

@FunctionalInterface
public interface SequenceHandler<T, R> extends Handler1<T, Sequence<R>> {
  
  default <V> SequenceHandler<T, V> map(Handler1<R, V> handler) {
    return value -> handle(value).map(handler::handle);
  }
  
  default <V> SequenceHandler<T, V> flatMap(SequenceHandler<R, V> handler) {
    return value -> handle(value).flatMap(handler::handle);
  }
  
  default SequenceHandler<T, R> filter(Matcher<R> matcher) {
    return value -> handle(value).filter(matcher::match);
  }

  default StreamHandler<T, R> toStreamHandler() {
    return value -> handle(value).stream();
  }
  
  static <T, R> SequenceHandler<T, R> adapt(Handler1<T, Sequence<R>> handler) {
    return handler::handle;
  }
}
