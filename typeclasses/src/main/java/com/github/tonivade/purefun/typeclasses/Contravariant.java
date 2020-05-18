/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface Contravariant<F extends Kind> extends Invariant<F> {

  <A, B> Higher1<F, B> contramap(Higher1<F, A> value, Function1<B, A> map);

  @Override
  default <A, B> Higher1<F, B> imap(Higher1<F, A> value, Function1<A, B> map, Function1<B, A> comap) {
    return contramap(value, comap);
  }

  static <F extends Kind, G extends Kind> Contravariant<Nested<F, G>> compose(Functor<F> f, Contravariant<G> g) {
    return new ComposedCovariantContravariant<F, G>() {
      @Override
      public Functor<F> f() { return f; }

      @Override
      public Contravariant<G> g() { return g; }
    };
  }

  static <F extends Kind, G extends Kind> Contravariant<Nested<F, G>> compose(Contravariant<F> f, Functor<G> g) {
    return new ComposedContravariantCovariant<F, G>() {
      @Override
      public Contravariant<F> f() { return f; }

      @Override
      public Functor<G> g() { return g; }
    };
  }
}
