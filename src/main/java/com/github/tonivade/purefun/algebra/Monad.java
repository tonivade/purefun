/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Witness;

public interface Monad<F extends Witness> extends Functor<F> {

  <T> Higher1<F, T> pure(T value);

  <T, R> Higher1<F, R> flatMap(Higher1<F, T> value, Function1<T, ? extends Higher1<F, R>> map);

  @Override
  default <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map) {
    return flatMap(value, map.andThen(this::pure));
  }
}