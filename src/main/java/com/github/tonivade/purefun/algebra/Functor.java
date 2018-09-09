/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.IdKind;

public interface Functor<F extends Witness> {

  <T, R> Higher<F, R> map(Higher<F, T> value, Function1<T, R> map);

  static Functor<IdKind.µ> id() {
    return new Functor<IdKind.µ>() {
      @Override
      public <T, R> Higher<IdKind.µ, R> map(Higher<IdKind.µ, T> value, Function1<T, R> map) {
        return IdKind.narrowK(value).map(map);
      }
    };
  }
}