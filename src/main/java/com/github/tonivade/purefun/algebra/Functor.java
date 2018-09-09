/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Id;

public interface Functor<F extends Witness> {

  <T, R> Higher<F, R> map(Higher<F, T> value, Function1<T, R> map);

  static Functor<Id.µ> id() {
    return new Functor<Id.µ>() {
      @Override
      public <T, R> Higher<Id.µ, R> map(Higher<Id.µ, T> value, Function1<T, R> map) {
        return Id.narrowK(value).map(map);
      }
    };
  }
}