/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface Profunctor<F extends Witness> {

  <A, B, C, D> Kind<Kind<F, C>, D> dimap(Kind<Kind<F, A>, ? extends B> value, 
      Function1<? super C, ? extends A> contramap, Function1<? super B, ? extends D> map);

  default <A, B, C> Kind<Kind<F, C>, B> lmap(Kind<Kind<F, A>, ? extends B> value, 
      Function1<? super C, ? extends A> contramap) {
    return dimap(value, contramap, identity());
  }

  default <A, B, D> Kind<Kind<F, A>, D> rmap(Kind<Kind<F, A>, ? extends B> value, 
      Function1<? super B, ? extends D> map) {
    return dimap(value, identity(), map);
  }
}
