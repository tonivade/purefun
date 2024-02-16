/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

@SuppressWarnings("unused")
public interface Nested<F extends Witness, G extends Witness> extends Witness {

  @SuppressWarnings("unchecked")
  static <F extends Witness, G extends Witness, A> Kind<Nested<F, G>, A> nest(Kind<F, ? extends Kind<G, ? extends A>> unnested) {
    return (Kind<Nested<F, G>, A>) unnested;
  }

  @SuppressWarnings("unchecked")
  static <F extends Witness, G extends Witness, A> Kind<F, Kind<G, A>> unnest(Kind<Nested<F, G>, ? extends A> nested) {
    return (Kind<F, Kind<G, A>>) nested;
  }
}
