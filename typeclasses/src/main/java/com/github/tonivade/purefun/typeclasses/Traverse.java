/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.type.IdOf.toId;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.type.Id;

public interface Traverse<F> extends Functor<F>, Foldable<F> {

  <G, T, R> Kind<G, Kind<F, R>> traverse(Applicative<G> applicative, Kind<F, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper);

  default <G, T> Kind<G, Kind<F, T>> sequence(Applicative<G> applicative,
      Kind<F, ? extends Kind<G, ? extends T>> value) {
    return traverse(applicative, value, identity());
  }

  @Override
  default <T, R> Kind<F, R> map(Kind<F, ? extends T> value, Function1<? super T, ? extends R> map) {
    Kind<Id<?>, Kind<F, R>> traverse = traverse(Instances.<Id<?>>applicative(), value, t -> Id.of(map.apply(t)));
    return traverse.fix(toId()).value();
  }

  static <F, G> Traverse<Nested<F, G>> compose(Traverse<F> f, Traverse<G> g) {
    return new ComposedTraverse<>() {

      @Override
      public Traverse<F> f() { return f; }

      @Override
      public Traverse<G> g() { return g; }
    };
  }
}
