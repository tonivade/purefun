/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.function.Supplier;

@FunctionalInterface
public interface TryHandler<T, R> extends Handler1<T, Try<R>> {
  
  default <V> TryHandler<T, V> map(Handler1<R, V> mapper) {
    return value -> handle(value).map(mapper::handle);
  }
  
  default <V> TryHandler<T, V> flatMap(Handler1<R, Try<V>> mapper) {
    return value -> handle(value).flatMap(mapper::handle);
  }
  
  default TryHandler<T, R> recover(Handler1<Throwable, R> handler) {
    return value -> handle(value).recover(handler);
  }
  
  default TryHandler<T, R> filter(Matcher<R> predicate) {
    return value -> handle(value).filter(predicate);
  }
  
  default Handler1<T, R> orElse(Supplier<R> supplier) {
    return value -> handle(value).orElse(supplier);
  }
  
  static <T, R> TryHandler<T, R> adapt(Handler1<T, Try<R>> handler) {
    return handler::handle;
  }
  
  static <T, R> TryHandler<T, R> adapt(Supplier<Try<R>> supplier) {
    return value -> supplier.get();
  }
}
