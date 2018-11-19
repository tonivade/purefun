/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface CheckedProducer<T> extends Recoverable {

  T get() throws Exception;

  default <V> CheckedFunction1<V, T> asFunction() {
    return value -> get();
  }
  
  default Producer<Try<T>> liftTry() {
    return () -> Try.of(this);
  }
  
  default Producer<Option<T>> liftOption() {
    return liftTry().andThen(Try::toOption);
  }
  
  default Producer<Either<Throwable, T>> liftEither() {
    return liftTry().andThen(Try::toEither);
  }

  default <R> CheckedProducer<R> andThen(CheckedFunction1<T, R> after) {
    return () -> after.apply(get());
  }

  default Producer<T> recover(Function1<Throwable, T> mapper) {
    return () -> {
      try {
        return get();
      } catch (Exception e) {
        return mapper.apply(e);
      }
    };
  }

  default Producer<T> unchecked() {
    return recover(this::sneakyThrow);
  }

  static <T> CheckedProducer<T> unit(T value) {
    return () -> value;
  }

  static <T, X extends Exception> CheckedProducer<T> failure(Producer<X> supplier) {
    return () -> { throw supplier.get(); };
  }

  static <T> CheckedProducer<T> of(CheckedProducer<T> reference) {
    return reference;
  }
}
