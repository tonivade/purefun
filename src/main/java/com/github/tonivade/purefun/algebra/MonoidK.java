/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;

public interface MonoidK<W extends Witness, T> extends SemigroupK<W, T>, Monoid<Higher1<W, T>> {

  static <W extends Witness, T> MonoidK<W, T> of(Producer<Higher1<W, T>> zero, SemigroupK<W, T> combine) {
    return new GenericMonoidK<>(zero, combine);
  }

  static <T> MonoidK<Sequence.µ, T> sequence() {
    return new GenericMonoidK<>(ImmutableList::empty, SemigroupK.sequence());
  }
}

class GenericMonoidK<W extends Witness, T> extends GenericMonoid<Higher1<W, T>> implements MonoidK<W, T> {
  GenericMonoidK(Producer<Higher1<W, T>> zero, SemigroupK<W, T> semigroup) {
    super(zero, semigroup);
  }
}
