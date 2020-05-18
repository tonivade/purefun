/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface Invariant<F extends Kind> {

  <A, B> Higher1<F, B> imap(Higher1<F, A> value, Function1<A, B> map, Function1<B, A> comap);

  static <F extends Kind, G extends Kind> Invariant<Nested<F, G>> compose(Invariant<F> f, Invariant<G> g) {
    return new ComposedInvariant<F, G>() {

      @Override
      public Invariant<F> f() { return f; }

      @Override
      public Invariant<G> g() { return g; }
    };
  }

  static <F extends Kind, G extends Kind> Invariant<Nested<F, G>> compose(Invariant<F> f, Functor<G> g) {
    return new ComposedInvariantCovariant<F, G>() {

      @Override
      public Invariant<F> f() { return f; }

      @Override
      public Functor<G> g() { return g; }
    };
  }

  static <F extends Kind, G extends Kind> Invariant<Nested<F, G>> compose(Invariant<F> f, Contravariant<G> g) {
    return new ComposedInvariantContravariant<F, G>() {

      @Override
      public Invariant<F> f() { return f; }

      @Override
      public Contravariant<G> g() { return g; }
    };
  }
}
