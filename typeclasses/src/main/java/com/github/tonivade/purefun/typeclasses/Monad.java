/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Function1.identity;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.type.Either;

public interface Monad<F> extends Selective<F> {

  <T, R> Kind<F, R> flatMap(
      Kind<F, ? extends T> value, Function1<? super T, ? extends Kind<F, ? extends R>> map);

  default For.FlatMap<F> use() {
    return For.with(this);
  }

  default <T, R> Kind<F, R> tailRecM(
      T value, Function1<T, ? extends Kind<F, Either<T, R>>> map) {
    return flatMap(map.apply(value), either -> either.fold(
        left -> tailRecM(left, map), this::<R>pure));
  }

  default <T, R> Kind<F, R> andThen(
      Kind<F, ? extends T> value, Producer<? extends Kind<F, ? extends R>> next) {
    return flatMap(value, ignore -> next.get());
  }

  default <T> Kind<F, T> flatten(Kind<F, Kind<F, ? extends T>> value) {
    return flatMap(value, identity());
  }

  @Override
  default <T, R> Kind<F, R> map(
      Kind<F, ? extends T> value, Function1<? super T, ? extends R> map) {
    return flatMap(value, map.andThen(this::<R>pure));
  }

  @Override
  default <T, R> Kind<F, R> ap(
      Kind<F, ? extends T> value, Kind<F, ? extends Function1<? super T, ? extends R>> apply) {
    return flatMap(apply, map -> map(value, map));
  }

  @Override
  default <A, B> Kind<F, B> select(
      Kind<F, Either<A, B>> value, Kind<F, Function1<? super A, ? extends B>> apply) {
    return flatMap(value, either -> either.fold(
        a -> map(apply, map -> map.apply(a)), this::<B>pure));
  }
}