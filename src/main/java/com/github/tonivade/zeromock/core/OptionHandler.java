/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.function.Supplier;

@FunctionalInterface
public interface OptionHandler<T, R> extends Handler1<T, Option<R>> {
  
  default <V> OptionHandler<T, V> map(Handler1<R, V> mapper) {
    return value -> handle(value).map(mapper::handle);
  }
  
  default <V> OptionHandler<T, V> flatMap(Handler1<R, Option<V>> mapper) {
    return value -> handle(value).flatMap(mapper::handle);
  }
  
  default OptionHandler<T, R> filter(Matcher<R> predicate) {
    return value -> handle(value).filter(predicate);
  }
  
  default Handler1<T, R> orElse(Supplier<R> supplier) {
    return value -> handle(value).orElse(supplier);
  }
}
