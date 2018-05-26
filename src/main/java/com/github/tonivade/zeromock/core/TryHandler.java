/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

@FunctionalInterface
public interface TryHandler<T, R> extends Handler1<T, Try<R>> {
  
  default <V> TryHandler<T, V> map(Handler1<R, V> handler) {
    return value -> handle(value).map(handler::handle);
  }
  
  default <V> TryHandler<T, V> flatMap(Handler1<R, Try<V>> handler) {
    return value -> handle(value).flatMap(handler::handle);
  }
  
  default TryHandler<T, R> recover(Handler1<Throwable, R> handler) {
    return value -> handle(value).recover(handler);
  }
  
  default TryHandler<T, R> filter(Matcher<R> matcher) {
    return value -> handle(value).filter(matcher);
  }
  
  default Handler1<T, R> orElse(Handler0<R> handler) {
    return value -> handle(value).orElse(handler);
  }
  
  static <T, R> TryHandler<T, R> adapt(Handler1<T, Try<R>> handler) {
    return handler::handle;
  }
}
