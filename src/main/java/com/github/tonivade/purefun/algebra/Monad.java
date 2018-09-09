/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id.µ;

public interface Monad<F extends Witness> extends Functor<F> {

  <T> Higher<F, T> pure(T value);

  <T, R> Higher<F, R> flatMap(Higher<F, T> value, Function1<T, ? extends Higher<F, R>> map);

  @Override
  default <T, R> Higher<F, R> map(Higher<F, T> value, Function1<T, R> map) {
    return flatMap(value, map.andThen(this::pure));
  }

  static Monad<Id.µ> id() {
    return new Monad<Id.µ>() {
      @Override
      public <T> Higher<µ, T> pure(T value) {
        return Id.of(value);
      }

      @Override
      public <T, R> Higher<Id.µ, R> flatMap(Higher<Id.µ, T> value,
                                                Function1<T, ? extends Higher<Id.µ, R>> map) {
        return Id.narrowK(value).flatMap(map);
      }
    };
  }
}