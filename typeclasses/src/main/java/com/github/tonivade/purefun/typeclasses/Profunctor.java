/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Function1.identity;

import com.github.tonivade.purefun.Kind2;
import com.github.tonivade.purefun.core.Function1;

public interface Profunctor<F> {

  <A, B, C, D> Kind2<F, C, D> dimap(Kind2<F, ? extends A, ? extends B> value,
      Function1<? super C, ? extends A> contramap, Function1<? super B, ? extends D> map);

  default <A, B, C> Kind2<F, C, B> lmap(Kind2<F, ? extends A, ? extends B> value,
      Function1<? super C, ? extends A> contramap) {
    return dimap(value, contramap, identity());
  }

  default <A, B, D> Kind2<F, A, D> rmap(Kind2<F, ? extends A, ? extends B> value,
      Function1<? super B, ? extends D> map) {
    return dimap(value, identity(), map);
  }
}
