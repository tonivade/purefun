/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.data.Sequence;

public interface SemigroupK<W extends Witness, T> extends Higher1<W, T>, Semigroup<Higher1<W, T>> {

  static <T> SemigroupK<Sequence.µ, T> sequence() {
    return (t1, t2) -> Sequence.narrowK(t1).appendAll(Sequence.narrowK(t2));
  }
}
