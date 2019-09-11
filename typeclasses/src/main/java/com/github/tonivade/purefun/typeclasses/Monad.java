/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.TypeClass;
import com.github.tonivade.purefun.type.Either;

import static com.github.tonivade.purefun.Function1.identity;

@TypeClass
public interface Monad<F extends Kind> extends Applicative<F> {

  <T, R> Higher1<F, R> flatMap(Higher1<F, T> value, Function1<T, ? extends Higher1<F, R>> map);

  // XXX: this method in not thread safe. In fact, now I don't really know how to do it thread safe
  // without real tail recursion optimization
  default <T, R> Higher1<F, R> tailRecM(T value, Function1<T, ? extends Higher1<F, Either<T, R>>> map) {
    return flatMap(map.apply(value), either -> either.fold(left -> tailRecM(left, map), this::<R>pure));
  }

  default <T, R> Higher1<F, R> andThen(Higher1<F, T> value, Producer<? extends Higher1<F, R>> next) {
    return flatMap(value, ignore -> next.get());
  }
  
  default <T> Higher1<F, T> flatten(Higher1<F, Higher1<F, T>> value) {
    return flatMap(value, identity());
  }

  @Override
  default <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map) {
    return flatMap(value, map.andThen(this::<R>pure));
  }

  @Override
  default <T, R> Higher1<F, R> ap(Higher1<F, T> value, Higher1<F, Function1<T, R>> apply) {
    return flatMap(apply, map -> map(value, map));
  }
}