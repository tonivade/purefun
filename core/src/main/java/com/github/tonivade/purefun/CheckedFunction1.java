/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface CheckedFunction1<A, R> extends Recoverable {

  R apply(A value) throws Throwable;

  default <B> CheckedFunction1<A, B> andThen(CheckedFunction1<R, B> after) {
    return (A value) -> after.apply(apply(value));
  }

  default <B> CheckedFunction1<B, R> compose(CheckedFunction1<B, A> before) {
    return (B value) -> apply(before.apply(value));
  }

  default Function1<A, R> recover(Function1<Throwable, R> mapper) {
    return value -> {
      try {
        return apply(value);
      } catch(Throwable e) {
        return mapper.apply(e);
      }
    };
  }

  default Function1<A, Option<R>> liftOption() {
    return liftTry().andThen(Try::toOption);
  }

  default Function1<A, Either<Throwable, R>> liftEither() {
    return liftTry().andThen(Try::toEither);
  }

  default Function1<A, Try<R>> liftTry() {
    return value -> Try.of(() -> apply(value));
  }

  default Function1<A, R> unchecked() {
    return recover(this::sneakyThrow);
  }

  static <T> CheckedFunction1<T, T> identity() {
    return value -> value;
  }

  static <T, X extends Exception> CheckedFunction1<T, T> failure(Producer<X> supplier) {
    return value -> { throw supplier.get(); };
  }

  static <T, R> CheckedFunction1<T, R> of(CheckedFunction1<T, R> reference) {
    return reference;
  }
}
