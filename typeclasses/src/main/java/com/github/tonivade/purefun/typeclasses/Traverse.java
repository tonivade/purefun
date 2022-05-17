/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.type.IdOf.toId;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;

public interface Traverse<F extends Witness> extends Functor<F>, Foldable<F> {

  <G extends Witness, T, R> Kind<G, Kind<F, R>> traverse(Applicative<G> applicative, Kind<F, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper);

  default <G extends Witness, T> Kind<G, Kind<F, T>> sequence(Applicative<G> applicative,
      Kind<F, ? extends Kind<G, ? extends T>> value) {
    return traverse(applicative, value, identity());
  }

  @Override
  default <T, R> Kind<F, R> map(Kind<F, ? extends T> value, Function1<? super T, ? extends R> map) {
    Kind<Id_, Kind<F, R>> traverse = traverse(Instances.<Id_>applicative(), value, t -> Id.of(map.apply(t)));
    return traverse.fix(toId()).get();
  }

  static <F extends Witness, G extends Witness> Traverse<Nested<F, G>> compose(Traverse<F> f, Traverse<G> g) {
    return new ComposedTraverse<>() {

      @Override
      public Traverse<F> f() { return f; }

      @Override
      public Traverse<G> g() { return g; }
    };
  }
}
