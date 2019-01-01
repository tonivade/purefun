/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
}
