/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.Optional;

@FunctionalInterface
public interface OptionalHandler<T, R> extends Function1<T, Optional<R>> {
  
  default <V> OptionalHandler<T, V> map(Function1<R, V> mapper) {
    return value -> apply(value).map(mapper::apply);
  }
  
  default <V> OptionalHandler<T, V> flatMap(OptionalHandler<R, V> mapper) {
    return value -> apply(value).flatMap(mapper::apply);
  }
  
  @SuppressWarnings("unchecked")
  default <V> OptionalHandler<T, V> flatten() {
    return value -> {
      Optional<R> result = apply(value);
      if (result.isPresent()) {
        return (Optional<V>) result.get();
      }
      return Optional.empty();
    };
  }
  
  default OptionalHandler<T, R> filter(Matcher<R> matcher) {
    return value -> apply(value).filter(matcher::match);
  }
  
  default Function1<T, R> orElse(Producer<R> producer) {
    return value -> apply(value).orElseGet(producer::get);
  }
}
