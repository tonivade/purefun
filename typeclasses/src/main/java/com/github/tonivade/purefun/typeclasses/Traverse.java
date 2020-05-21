/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.IdOf;
import com.github.tonivade.purefun.type.Id_;

public interface Traverse<F extends Witness> extends Functor<F>, Foldable<F> {

  <G extends Witness, T, R> Kind<G, Kind<F, R>> traverse(Applicative<G> applicative, Kind<F, T> value,
      Function1<T, ? extends Kind<G, R>> mapper);

  default <G extends Witness, T> Kind<G, Kind<F, T>> sequence(Applicative<G> applicative,
      Kind<F, Kind<G, T>> value) {
    return traverse(applicative, value, identity());
  }

  @Override
  default <T, R> Kind<F, R> map(Kind<F, T> value, Function1<T, R> map) {
    return IdOf.narrowK(traverse(IdApplicative.INSTANCE, value, t -> Id.of(map.apply(t)))).get();
  }

  static <F extends Witness, G extends Witness> Traverse<Nested<F, G>> compose(Traverse<F> f, Traverse<G> g) {
    return new ComposedTraverse<F, G>() {

      @Override
      public Traverse<F> f() { return f; }

      @Override
      public Traverse<G> g() { return g; }
    };
  }
}

interface IdApplicative extends Applicative<Id_> {

  IdApplicative INSTANCE = new IdApplicative() {};

  @Override
  default <T> Kind<Id_, T> pure(T value) {
    return Id.of(value);
  }

  @Override
  default <T, R> Kind<Id_, R> ap(Kind<Id_, T> value, Kind<Id_, Function1<T, R>> apply) {
    return IdOf.narrowK(value).flatMap(t -> IdOf.narrowK(apply).map(f -> f.apply(t)));
  }
}
