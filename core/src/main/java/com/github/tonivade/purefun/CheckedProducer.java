/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface CheckedProducer<A> extends Recoverable {

  A get() throws Throwable;

  default <B> CheckedFunction1<B, A> asFunction() {
    return value -> get();
  }

  default Producer<Try<A>> liftTry() {
    return () -> Try.of(this);
  }

  default Producer<Option<A>> liftOption() {
    return liftTry().andThen(Try::toOption);
  }

  default Producer<Either<Throwable, A>> liftEither() {
    return liftTry().andThen(Try::toEither);
  }

  default <R> CheckedProducer<R> andThen(CheckedFunction1<A, R> after) {
    return () -> after.apply(get());
  }

  default Producer<A> recover(Function1<Throwable, A> mapper) {
    return () -> {
      try {
        return get();
      } catch (Throwable e) {
        return mapper.apply(e);
      }
    };
  }

  default Producer<A> unchecked() {
    return recover(this::sneakyThrow);
  }

  static <A> CheckedProducer<A> cons(A value) {
    return () -> value;
  }

  static <A, X extends Throwable> CheckedProducer<A> failure(Producer<X> supplier) {
    return () -> { throw supplier.get(); };
  }

  static <A> CheckedProducer<A> of(CheckedProducer<A> reference) {
    return reference;
  }
}
