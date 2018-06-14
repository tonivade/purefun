/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
public interface OptionalHandler<T, R> extends Function1<T, Optional<R>> {
  
  default <V> OptionalHandler<T, V> map(Function1<R, V> mapper) {
    return value -> apply(value).map(mapper::apply);
  }
  
  default <V> OptionalHandler<T, V> flatMap(OptionalHandler<R, V> mapper) {
    return value -> apply(value).flatMap(mapper::apply);
  }
  
  default OptionalHandler<T, R> filter(Predicate<R> predicate) {
    return value -> apply(value).filter(predicate);
  }
  
  default Function1<T, R> orElse(Supplier<R> supplier) {
    return value -> apply(value).orElseGet(supplier);
  }
}
