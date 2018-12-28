/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;

public interface Monoid<T> extends Semigroup<T> {

  T zero();

  static <T> Monoid<Sequence<T>> sequence() {
    return new Monoid<Sequence<T>>() {

      @Override
      public Sequence<T> combine(Sequence<T> t1, Sequence<T> t2) {
        return Sequence.narrowK(t1).appendAll(Sequence.narrowK(t2));
      }

      @Override
      public Sequence<T> zero() {
        return ImmutableList.empty();
      }
    };
  }
}