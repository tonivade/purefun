/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

@FunctionalInterface
public interface EitherHandler<T, L, R> extends Handler1<T, Either<L, R>>{

  default <V> EitherHandler<T, L, V> map(Handler1<R, V> handler) {
    return value -> handle(value).map(handler::handle);
  }
  
  default <V> EitherHandler<T, V, R> mapLeft(Handler1<L, V> handler) {
    return value -> handle(value).mapLeft(handler::handle);
  }
  
  default <V> EitherHandler<T, L, V> flatMap(Handler1<R, Either<L, V>> handler) {
    return value -> handle(value).flatMap(handler::handle);
  }
  
  default OptionHandler<T, Either<L, R>> filter(Matcher<R> matcher) {
    return value -> handle(value).filter(matcher);
  }
  
  default Handler1<T, R> orElse(Handler0<R> handler) {
    return value -> handle(value).orElse(handler);
  }
  
  static <T, L, R> EitherHandler<T, L, R> adapt(Handler1<T, Either<L, R>> handler) {
    return handler::handle;
  }
}
