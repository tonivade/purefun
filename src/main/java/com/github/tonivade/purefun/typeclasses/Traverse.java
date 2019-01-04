/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nested.unnest;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.type.Id;

public interface Traverse<F extends Kind> extends Functor<F> {

  <G extends Kind, T, R> Higher1<G, Higher1<F, R>> traverse(Applicative<G> applicative, Higher1<F, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper);

  @Override
  default <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map) {
    return Id.narrowK(traverse(Id.applicative(), value, t -> Id.of(map.apply(t)))).get();
  }

  static <F extends Kind, G extends Kind> Traverse<Nested<F, G>> compose(Traverse<F> tf, Traverse<G> tg) {
    return new Traverse<Nested<F,G>>() {

      @Override
      public <H extends Kind, T, R> Higher1<H, Higher1<Nested<F, G>, R>> traverse(Applicative<H> applicative,
          Higher1<Nested<F, G>, T> value, Function1<T, ? extends Higher1<H, R>> mapper) {
        return applicative.map(
            tf.traverse(applicative, unnest(value), ga -> tg.traverse(applicative, ga, mapper)),
            Nested::nest);
      }
    };
  }
}
