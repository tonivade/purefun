/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.data.Sequence.listOf;

import java.util.Optional;
import java.util.stream.Stream;

import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface Function1<T, R> {

  R apply(T value);

  default <V> Function1<T, V> andThen(Function1<R, V> after) {
    return value -> after.apply(apply(value));
  }

  default <V> Function1<V, R> compose(Function1<V, T> before) {
    return value -> apply(before.apply(value));
  }

  default Function1<T, Optional<R>> liftOptional() {
    return value -> Optional.ofNullable(apply(value));
  }

  default Function1<T, Option<R>> liftOption() {
    return value -> Option.of(() -> apply(value));
  }

  default Function1<T, Try<R>> liftTry() {
    return value -> Try.of(() -> apply(value));
  }

  default Function1<T, Either<Throwable, R>> liftEither() {
    return liftTry().andThen(Try::toEither);
  }

  default <L> Function1<T, Either<L, R>> liftRight() {
    return value -> Either.right(apply(value));
  }

  default <L> Function1<T, Either<R, L>> liftLeft() {
    return value -> Either.left(apply(value));
  }

  default Function1<T, Sequence<R>> sequence() {
    return value -> listOf(apply(value));
  }

  default Function1<T, Stream<R>> stream() {
    return value -> Stream.of(apply(value));
  }

  default CheckedFunction1<T, R> checked() {
    return this::apply;
  }

  default Function1<T, R> memoized() {
    return new MemoizedFunction<>(this);
  }

  default PartialFunction1<T, R> partial(Matcher1<T> isDefined) {
    return new PartialFunction1<T, R>() {
      @Override
      public boolean isDefinedAt(T value) {
        return isDefined.match(value);
      }

      @Override
      public R apply(T value) {
        return Function1.this.apply(value);
      }
    };
  }

  static <T> Function1<T, T> identity() {
    return value -> value;
  }

  static <T, R> Function1<T, R> of(Function1<T, R> reference) {
    return reference;
  }
}
