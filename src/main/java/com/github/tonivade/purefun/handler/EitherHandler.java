/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.handler;

import static com.github.tonivade.purefun.type.EitherKind.narrowK;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Matcher;
import com.github.tonivade.purefun.Monad2;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.EitherKind;

@FunctionalInterface
public interface EitherHandler<T, L, R> extends Function1<T, Monad2<EitherKind.µ, L, R>>{

  default Either<L, R> applyK(T value) {
    return narrowK(apply(value));
  }

  @Override
  default <V> EitherHandler<V, L, R> compose(Function1<V, T> before) {
    return value -> applyK(before.apply(value));
  }

  default <V> EitherHandler<T, L, V> map(Function1<R, V> mapper) {
    return value -> applyK(value).map(mapper::apply);
  }

  default <V> EitherHandler<T, V, R> mapLeft(Function1<L, V> mapper) {
    return value -> applyK(value).mapLeft(mapper::apply);
  }

  default <V> EitherHandler<T, L, V> flatMap(EitherHandler<R, L, V> mapper) {
    return value -> applyK(value).flatMap(mapper::apply);
  }

  default <V> EitherHandler<T, L, V> flatten() {
    return value -> applyK(value).flatten();
  }

  default OptionHandler<T, Either<L, R>> filter(Matcher<R> matcher) {
    return value -> applyK(value).filter(matcher);
  }

  default Function1<T, R> orElse(Producer<R> producer) {
    return value -> applyK(value).orElse(producer);
  }

  static <L, R> Function1<Either<L, R>, Either<L, R>> identity() {
    return Function1.<Either<L, R>>identity()::apply;
  }

  static <T, L, R> EitherHandler<T, L, R> of(Function1<T, Either<L, R>> reference) {
    return reference::apply;
  }
}
