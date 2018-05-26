/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

@FunctionalInterface
public interface OptionHandler<T, R> extends Handler1<T, Option<R>> {
  
  default <V> OptionHandler<T, V> map(Handler1<R, V> handler) {
    return value -> handle(value).map(handler::handle);
  }
  
  default <V> OptionHandler<T, V> flatMap(Handler1<R, Option<V>> handler) {
    return value -> handle(value).flatMap(handler::handle);
  }
  
  default OptionHandler<T, R> filter(Matcher<R> matcher) {
    return value -> handle(value).filter(matcher);
  }
  
  default Handler1<T, R> orElse(Handler0<R> handler) {
    return value -> handle(value).orElse(handler);
  }
  
  static <T, R> OptionHandler<T, R> adapt(Handler1<T, Option<R>> handler) {
    return handler::handle;
  }
}
