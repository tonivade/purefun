/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface OptionalHandler<T, R> extends Handler1<T, Optional<R>> {
  
  default <V> OptionalHandler<T, V> map(Handler1<R, V> mapper) {
    return value -> handle(value).map(mapper::handle);
  }
  
  default <V> OptionalHandler<T, V> flatMap(Handler1<R, Optional<V>> mapper) {
    return value -> handle(value).flatMap(mapper::handle);
  }
  
  default OptionalHandler<T, R> filter(Predicate<R> predicate) {
    return value -> handle(value).filter(predicate);
  }
  
  default Handler1<T, R> orElse(Supplier<R> supplier) {
    return value -> handle(value).orElseGet(supplier);
  }
}
