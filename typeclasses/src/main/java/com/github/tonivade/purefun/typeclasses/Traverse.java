/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.TypeClass;
import com.github.tonivade.purefun.type.Id;

@TypeClass
public interface Traverse<F extends Kind> extends Functor<F>, Foldable<F> {

  <G extends Kind, T, R> Higher1<G, Higher1<F, R>> traverse(Applicative<G> applicative, Higher1<F, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper);

  default <G extends Kind, T> Higher1<G, Higher1<F, T>> sequence(Applicative<G> applicative,
      Higher1<F, Higher1<G, T>> value) {
    return traverse(applicative, value, identity());
  }

  @Override
  default <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map) {
    return Id.narrowK(traverse(new IdApplicative(), value, t -> Id.of(map.apply(t)))).get();
  }

  static <F extends Kind, G extends Kind> Traverse<Nested<F, G>> compose(Traverse<F> f, Traverse<G> g) {
    return new ComposedTraverse<F, G>() {

      @Override
      public Traverse<F> f() { return f; }

      @Override
      public Traverse<G> g() { return g; }
    };
  }
}

class IdApplicative implements Applicative<Id.µ> {

  @Override
  public <T> Id<T> pure(T value) {
    return Id.of(value);
  }

  @Override
  public <T, R> Id<R> ap(Higher1<Id.µ, T> value, Higher1<Id.µ, Function1<T, R>> apply) {
    return Id.narrowK(value).flatMap(t -> Id.narrowK(apply).map(f -> f.apply(t)));
  }
}