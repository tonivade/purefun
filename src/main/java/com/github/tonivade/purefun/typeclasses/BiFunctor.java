/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;

public interface BiFunctor<F extends Kind> {

  <A, B, C, D> Higher2<F, C, D> bimap(Higher2<F, A, B> value, Function1<A, C> leftMap, Function1<B, D> rightMap);

  default <A, B, C> Higher2<F, A, C> map(Higher2<F, A, B> value, Function1<B, C> map) {
    return bimap(value, Function1.identity(), map);
  }

  default <A, B, C> Higher2<F, C, B> leftMap(Higher2<F, A, B> value, Function1<A, C> map) {
    return bimap(value, map, Function1.identity());
  }
}
