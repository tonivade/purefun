/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface Handler1<T, R> {

  R handle(T value);
  
  default <V> Handler1<T, V> andThen(Handler1<R, V> after) {
    return (T value) -> after.handle(handle(value));
  }
  
  default <V> Handler1<V, R> compose(Handler1<V, T> before) {
    return (V value) -> handle(before.handle(value));
  }
  
  default OptionalHandler<T, R> lift() {
    return value -> Optional.of(handle(value));
  }
  
  static <T, R> Handler1<T, R> adapt(Supplier<R> supplier) {
    return value -> supplier.get();
  }
  
  static <T> Handler1<T, T> adapt(Consumer<T> consumer) {
    return value -> { consumer.accept(value); return value; };
  }

  static <T, R> Handler1<T, R> adapt(Function<T, R> function) {
    return function::apply;
  }
  
  static <T> Handler1<T, T> identity() {
    return value -> value;
  }
}
