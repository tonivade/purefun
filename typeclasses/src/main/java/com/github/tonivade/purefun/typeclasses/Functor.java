/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.TypeClass;

@TypeClass
public interface Functor<F extends Kind> extends Invariant<F> {

  <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map);

  default <A, B> Function1<Higher1<F, A>, Higher1<F, B>> lift(Function1<A, B> function) {
    return fa -> map(fa, function);
  }

  @Override
  default <A, B> Higher1<F, B> imap(Higher1<F, A> value, Function1<A, B> map, Function1<B, A> comap) {
    return map(value, map);
  }

  static <F extends Kind, G extends Kind> Functor<Nested<F, G>> compose(Functor<F> f, Functor<G> g) {
    return new ComposedFunctor<F, G>() {

      @Override
      public Functor<F> f() { return f; }

      @Override
      public Functor<G> g() { return g; }
    };
  }

  static <F extends Kind, G extends Kind> Functor<Nested<F, G>> compose(Contravariant<F> f, Contravariant<G> g) {
    return new ComposedContravariant<F, G>() {

      @Override
      public Contravariant<F> f() { return f; }

      @Override
      public Contravariant<G> g() { return g; }
    };
  }
}