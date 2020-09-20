/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Either;

public interface Monad<F extends Witness> extends Selective<F> {

  <T, R> Kind<F, R> flatMap(Kind<F, T> value, Function1<T, ? extends Kind<F, R>> map);

  // XXX: this method in not stack safe. In fact, now I don't really know how to do it stack safe
  // without real tail recursion optimization
  default <T, R> Kind<F, R> tailRecM(T value, Function1<T, ? extends Kind<F, Either<T, R>>> map) {
    return flatMap(map.apply(value), either -> either.fold(left -> tailRecM(left, map), this::<R>pure));
  }

  default <T, R> Kind<F, R> andThen(Kind<F, T> value, Producer<? extends Kind<F, R>> next) {
    return flatMap(value, ignore -> next.get());
  }
  
  default <T> Kind<F, T> flatten(Kind<F, Kind<F, T>> value) {
    return flatMap(value, identity());
  }

  @SuppressWarnings("unchecked")
  @Override
  default <T, R> Kind<F, R> map(Kind<F, T> value, Function1<? super T, ? extends R> map) {
    // TODO:
    Function1<T, ? extends Kind<F, R>> andThen = (Function1<T, ? extends Kind<F, R>>) map.andThen(this::<R>pure);
    return flatMap(value, andThen);
  }

  @Override
  default <T, R> Kind<F, R> ap(Kind<F, T> value, Kind<F, Function1<T, R>> apply) {
    return flatMap(apply, map -> map(value, map));
  }

  @Override
  default <A, B> Kind<F, B> select(Kind<F, Either<A, B>> value, Kind<F, Function1<A, B>> apply) {
    return flatMap(value, either -> either.fold(a -> map(apply, map -> map.apply(a)), this::<B>pure));
  }
}