/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.Sequence;

public interface SemigroupK<F extends Kind> {

  <T> Higher1<F, T> combineK(Higher1<F, T> t1, Higher1<F, T> t2);

  static SemigroupK<Sequence.µ> sequence() {
    return new SemigroupK<Sequence.µ>() {
      @Override
      public <T> Sequence<T> combineK(Higher1<Sequence.µ, T> t1, Higher1<Sequence.µ, T> t2) {
        return Sequence.narrowK(t1).appendAll(Sequence.narrowK(t2));
      }
    };
  }
}
