/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;

public interface Profunctor<F extends Kind> {

  <A, B, C, D> Higher2<F, C, D> dimap(Higher2<F, A, B> value, Function1<C, A> contramap, Function1<B, D> map);
  
  default <A, B, C> Higher2<F, C, B> lmap(Higher2<F, A, B> value, Function1<C, A> contramap) {
    return dimap(value, contramap, identity());
  }
  
  default <A, B, D> Higher2<F, A, D> rmap(Higher2<F, A, B> value, Function1<B, D> map) {
    return dimap(value, identity(), map);
  }
}
