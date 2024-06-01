/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;

public interface Invariant<F extends Kind<F, ?>> {

  <A, B> Kind<F, B> imap(Kind<F, ? extends A> value, Function1<? super A, ? extends B> map, Function1<? super B, ? extends A> comap);

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> Invariant<Nested<F, G>> compose(Invariant<F> f, Invariant<G> g) {
    return new ComposedInvariant<>() {

      @Override
      public Invariant<F> f() { return f; }

      @Override
      public Invariant<G> g() { return g; }
    };
  }

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> Invariant<Nested<F, G>> compose(Invariant<F> f, Functor<G> g) {
    return new ComposedInvariantCovariant<>() {

      @Override
      public Invariant<F> f() { return f; }

      @Override
      public Functor<G> g() { return g; }
    };
  }

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> Invariant<Nested<F, G>> compose(Invariant<F> f, Contravariant<G> g) {
    return new ComposedInvariantContravariant<>() {

      @Override
      public Invariant<F> f() { return f; }

      @Override
      public Contravariant<G> g() { return g; }
    };
  }
}
