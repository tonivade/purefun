/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface CheckedProducer<T> {

  T get() throws Exception;

  default <V> CheckedFunction1<V, T> asFunction() {
    return value -> get();
  }

  default <R> CheckedProducer<R> andThen(CheckedFunction1<T, R> after) {
    return () -> after.apply(get());
  }

  static <T> CheckedProducer<T> unit(T value) {
    return () -> value;
  }

  default Producer<Try<T>> liftTry() {
    return () -> Try.of(() -> get());
  }

  default Producer<Either<Throwable, T>> liftEither() {
    return liftTry().andThen(Try::toEither);
  }

  default Producer<Option<T>> liftOption() {
    return liftTry().andThen(Try::toOption);
  }

  static <T> CheckedProducer<T> of(CheckedProducer<T> reference) {
    return reference;
  }
}
