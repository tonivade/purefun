/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Producer.unit;

@FunctionalInterface
public interface TryHandler<T, R> extends Function1<T, Try<R>> {
  
  default <V> TryHandler<T, V> map(Function1<R, V> mapper) {
    return value -> apply(value).map(mapper::apply);
  }
  
  default <V> TryHandler<T, V> flatMap(TryHandler<R, V> mapper) {
    return value -> apply(value).flatMap(mapper::apply);
  }
  
  default TryHandler<T, R> recover(Function1<Throwable, R> mapper) {
    return value -> apply(value).recover(mapper);
  }
  
  default TryHandler<T, R> filter(Matcher<R> matcher) {
    return value -> apply(value).filter(matcher);
  }
  
  default Function1<T, R> orElse(R value) {
    return orElse(unit(value));
  }
  
  default Function1<T, R> orElse(Producer<R> producer) {
    return value -> apply(value).orElse(producer);
  }
}
