/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

public interface Contravariant<F extends Witness> extends Invariant<F> {

  <A, B> Kind<F, B> contramap(Kind<F, ? extends A> value, Function1<? super B, ? extends A> map);

  @Override
  default <A, B> Kind<F, B> imap(Kind<F, ? extends A> value, Function1<? super A, ? extends B> map, Function1<? super B, ? extends A> comap) {
    return contramap(value, comap);
  }

  static <F extends Witness, G extends Witness> Contravariant<Nested<F, G>> compose(Functor<F> f, Contravariant<G> g) {
    return new ComposedCovariantContravariant<F, G>() {
      @Override
      public Functor<F> f() { return f; }

      @Override
      public Contravariant<G> g() { return g; }
    };
  }

  static <F extends Witness, G extends Witness> Contravariant<Nested<F, G>> compose(Contravariant<F> f, Functor<G> g) {
    return new ComposedContravariantCovariant<F, G>() {
      @Override
      public Contravariant<F> f() { return f; }

      @Override
      public Functor<G> g() { return g; }
    };
  }
}
