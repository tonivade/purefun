/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;

public interface MonoidK<F extends Kind> extends SemigroupK<F> {

  <T> Higher1<F, T> zero();

  static MonoidK<Sequence.µ> sequence() {
    return new MonoidK<Sequence.µ>() {

      @Override
      public <T> Sequence<T> combineK(Higher1<Sequence.µ, T> t1, Higher1<Sequence.µ, T> t2) {
        return Sequence.narrowK(t1).appendAll(Sequence.narrowK(t2));
      }

      @Override
      public <T> Sequence<T> zero() {
        return ImmutableList.empty();
      }
    };
  }
}