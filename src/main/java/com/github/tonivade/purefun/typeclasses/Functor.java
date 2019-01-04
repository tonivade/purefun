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

public interface Functor<F extends Kind> {

  <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map);

  static <F extends Kind, G extends Kind> Functor<Nested<F, G>> compose(Functor<F> f, Functor<G> g) {
    return new Functor<Nested<F, G>>() {

      @Override
      public <T, R> Higher1<Nested<F, G>, R> map(Higher1<Nested<F, G>, T> value, Function1<T, R> map) {
        return nest(f.map(unnest(value), ga -> g.map(ga, map)));
      }
    };
  }
}