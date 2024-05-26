/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Function1.identity;

import com.github.tonivade.purefun.Kind2;
import com.github.tonivade.purefun.core.Function1;

public interface Bifunctor<F> {

  <A, B, C, D> Kind2<F, C, D> bimap(Kind2<F, ? extends A, ? extends B> value,
      Function1<? super A, ? extends C> leftMap, Function1<? super B, ? extends D> rightMap);

  default <A, B, C> Kind2<F, A, C> map(Kind2<F, ? extends A, ? extends B> value,
      Function1<? super B, ? extends C> map) {
    return bimap(value, identity(), map);
  }

  default <A, B, C> Kind2<F, C, B> leftMap(Kind2<F, ? extends A, ? extends B> value,
      Function1<? super A, ? extends C> map) {
    return bimap(value, map, identity());
  }
}
