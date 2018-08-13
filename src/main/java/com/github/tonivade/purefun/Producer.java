/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface Producer<T> {
  
  T get();
  
  default <V> Function1<V, T> asFunction() {
    return value -> get();
  }
  
  default <R> Producer<R> andThen(Function1<T, R> after) {
    return () -> after.apply(get());
  }
  
  default Producer<Option<T>> liftOption() {
    return () -> Option.of(this::get);
  }
  
  default Producer<Try<T>> liftTry() {
    return () -> Try.of(this::get);
  }
  
  default Producer<Either<Throwable, T>> liftEither() {
    return () -> Try.of(this::get).toEither();
  }
  
  static <T> Producer<T> unit(T value) {
    return () -> value;
  }
  
  static <T> Producer<T> of(Producer<T> reference) {
    return reference;
  }
}
