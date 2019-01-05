/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nested.nest;
import static com.github.tonivade.purefun.Nested.unnest;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nested;

public interface Alternative<F extends Kind> extends Applicative<F>, MonoidK<F> {

  static <F extends Kind, G extends Kind> Alternative<Nested<F, G>> compose(Alternative<F> f, Alternative<G> g) {
    return new Alternative<Nested<F,G>>() {

      @Override
      public <T> Higher1<Nested<F, G>, T> combineK(Higher1<Nested<F, G>, T> t1, Higher1<Nested<F, G>, T> t2) {
        return nest(f.combineK(unnest(t1), unnest(t2)));
      }

      @Override
      public <T> Higher1<Nested<F, G>, T> zero() {
        return nest(f.zero());
      }

      @Override
      public <T> Higher1<Nested<F, G>, T> pure(T value) {
        return nest(f.pure(g.pure(value)));
      }

      @Override
      public <T, R> Higher1<Nested<F, G>, R> ap(Higher1<Nested<F, G>, T> value,
          Higher1<Nested<F, G>, Function1<T, R>> apply) {
        return nest(f.ap(unnest(value), f.map(unnest(apply), gfa -> ga -> g.ap(ga, gfa))));
      }
    };
  }
}
