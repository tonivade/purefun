/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

/**
 * <p>This interface represents a function with a two parameters. Similar to {@link java.util.function.BiFunction}.</p>
 * <p>The function can throws checked exceptions, but calling {@code apply()} method, the exception is sneaky thrown. So, it
 * can be used as a higher order function in {@link java.util.stream.Stream} or {@link java.util.Optional} API.</p>
 * @param <A> type of first function parameter
 * @param <B> type of second function parameter
 * @param <R> type of return value
 */
@FunctionalInterface
public interface Function2<A, B, R> extends Recoverable {

  default R apply(A a, B b) {
    try {
      return run(a, b);
    } catch (Throwable t) {
      return sneakyThrow(t);
    }
  }

  R run(A a, B b) throws Throwable;

  default Function1<A, Function1<B, R>> curried() {
    return a -> b -> apply(a, b);
  }

  default Function1<Tuple2<A, B>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2());
  }

  default <C> Function2<A, B, C> andThen(Function1<R, C> after) {
    return (a, b) -> after.apply(apply(a, b));
  }

  default <C> Function1<C, R> compose(Function1<C, A> beforeT, Function1<C, B> beforeV) {
    return value -> apply(beforeT.apply(value), beforeV.apply(value));
  }

  default Function2<A, B, Try<R>> liftTry() {
    return (a, b) -> Try.of(() -> apply(a, b));
  }

  default Function2<A, B, Either<Throwable, R>> liftEither() {
    return liftTry().andThen(Try::toEither);
  }

  default Function2<A, B, Option<R>> liftOption() {
    return liftTry().andThen(Try::toOption);
  }

  default Function2<A, B, R> memoized() {
    return (a, b) -> new MemoizedFunction<>(tupled()).apply(Tuple.of(a, b));
  }

  static <A, B, R> Function2<A, B, R> of(Function2<A, B, R> reference) {
    return reference;
  }

  static <A, B, R> Function2<A, B, R> uncurried(Function1<A, Function1<B, R>> function) {
    return (a, b) -> function.apply(a).apply(b);
  }

  static <A, B, R> Function2<A, B, R> cons(R value) {
    return (a, b) -> value;
  }
}
