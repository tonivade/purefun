/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface Invariant<F extends Witness> {

  <A, B> Kind<F, B> imap(Kind<F, ? extends A> value, Function1<? super A, ? extends B> map, Function1<? super B, ? extends A> comap);

  static <F extends Witness, G extends Witness> Invariant<Nested<F, G>> compose(Invariant<F> f, Invariant<G> g) {
    return new ComposedInvariant<>() {

      @Override
      public Invariant<F> f() { return f; }

      @Override
      public Invariant<G> g() { return g; }
    };
  }

  static <F extends Witness, G extends Witness> Invariant<Nested<F, G>> compose(Invariant<F> f, Functor<G> g) {
    return new ComposedInvariantCovariant<>() {

      @Override
      public Invariant<F> f() { return f; }

      @Override
      public Functor<G> g() { return g; }
    };
  }

  static <F extends Witness, G extends Witness> Invariant<Nested<F, G>> compose(Invariant<F> f, Contravariant<G> g) {
    return new ComposedInvariantContravariant<>() {

      @Override
      public Invariant<F> f() { return f; }

      @Override
      public Contravariant<G> g() { return g; }
    };
  }
}
