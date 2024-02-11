/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Function1.identity;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;

public interface Bifunctor<F extends Witness> {

  <A, B, C, D> Kind<Kind<F, C>, D> bimap(Kind<Kind<F, A>, ? extends B> value, 
      Function1<? super A, ? extends C> leftMap, Function1<? super B, ? extends D> rightMap);

  default <A, B, C> Kind<Kind<F, A>, C> map(Kind<Kind<F, A>, ? extends B> value, 
      Function1<? super B, ? extends C> map) {
    return bimap(value, identity(), map);
  }

  default <A, B, C> Kind<Kind<F, C>, B> leftMap(Kind<Kind<F, A>, ? extends B> value, 
      Function1<? super A, ? extends C> map) {
    return bimap(value, map, identity());
  }
}
