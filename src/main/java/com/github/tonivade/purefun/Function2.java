/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface Function2<A, B, R> {

  R apply(A a, B b);

  default Function1<A, Function1<B, R>> curried() {
    return a -> b -> apply(a, b);
  }

  default Function1<Tuple2<A, B>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2());
  }

  default <U> Function2<A, B, U> andThen(Function1<R, U> after) {
    return (a, b) -> after.apply(apply(a, b));
  }

  default <U> Function1<U, R> compose(Function1<U, A> beforeT, Function1<U, B> beforeV) {
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

  static <T, V, R> Function2<T, V, R> of(Function2<T, V, R> reference) {
    return reference;
  }
}
