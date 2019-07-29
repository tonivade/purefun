/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;

public interface MonoidInvariant extends Invariant<Monoid.µ> {

  @Override
  default <A, B> Monoid<B> imap(Higher1<Monoid.µ, A> value,
                                Function1<A, B> map,
                                Function1<B, A> comap) {
    return new Monoid<B>() {

      @Override
      public B zero() {
        return map.apply(value.fix1(Monoid::narrowK).zero());
      }

      @Override
      public B combine(B t1, B t2) {
        return map.apply(value.fix1(Monoid::narrowK).combine(comap.apply(t1), comap.apply(t2)));
      }
    };
  }
}
