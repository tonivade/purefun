/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nested;

public interface Functor<F extends Kind> {

  <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map);

  static <F extends Kind, G extends Kind> Functor<Nested<F, G>> compose(Functor<F> f, Functor<G> g) {
    return new ComposedFunctor<F, G>() {

      @Override
      public Functor<F> f() { return f; }

      @Override
      public Functor<G> g() { return g; }
    };
  }
}