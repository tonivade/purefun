/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

public interface Profunctor<F extends Witness> {

  <A, B, C, D> Kind<Kind<F, C>, D> dimap(Kind<Kind<F, A>, B> value, Function1<C, A> contramap, Function1<B, D> map);

  default <A, B, C> Kind<Kind<F, C>, B> lmap(Kind<Kind<F, A>, B> value, Function1<C, A> contramap) {
    return dimap(value, contramap, identity());
  }

  default <A, B, D> Kind<Kind<F, A>, D> rmap(Kind<Kind<F, A>, B> value, Function1<B, D> map) {
    return dimap(value, identity(), map);
  }
}
