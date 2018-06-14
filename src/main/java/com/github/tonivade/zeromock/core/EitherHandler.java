/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

@FunctionalInterface
public interface EitherHandler<T, L, R> extends Function1<T, Either<L, R>>{

  default <V> EitherHandler<T, L, V> map(Function1<R, V> handler) {
    return value -> apply(value).map(handler::apply);
  }
  
  default <V> EitherHandler<T, V, R> mapLeft(Function1<L, V> handler) {
    return value -> apply(value).mapLeft(handler::apply);
  }
  
  default <V> EitherHandler<T, L, V> flatMap(EitherHandler<R, L, V> handler) {
    return value -> apply(value).flatMap(handler::apply);
  }
  
  default OptionHandler<T, Either<L, R>> filter(Matcher<R> matcher) {
    return value -> apply(value).filter(matcher);
  }
  
  default Function1<T, R> orElse(Producer<R> handler) {
    return value -> apply(value).orElse(handler);
  }
}
