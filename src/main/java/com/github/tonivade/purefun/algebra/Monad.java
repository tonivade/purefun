/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Witness;

public interface Monad<F extends Witness> extends Functor<F> {

  <T> Higher<F, T> pure(T value);

  <T, R> Higher<F, R> flatMap(Higher<F, T> value, Function1<T, ? extends Higher<F, R>> map);

  @Override
  default <T, R> Higher<F, R> map(Higher<F, T> value, Function1<T, R> map) {
    return flatMap(value, map.andThen(this::pure));
  }
}