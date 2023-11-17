/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Option;

public interface Foldable<F extends Witness> {

  <A, B> B foldLeft(Kind<F, ? extends A> value, B initial, Function2<? super B, ? super A, ? extends B> mapper);

  <A, B> Eval<B> foldRight(Kind<F, ? extends A> value, Eval<? extends B> initial, 
      Function2<? super A, ? super Eval<? extends B>, ? extends Eval<? extends B>> mapper);

  default <A> A fold(Monoid<A> monoid, Kind<F, ? extends A> value) {
    return foldMap(monoid, value, identity());
  }

  default <A, B> B foldMap(Monoid<B> monoid, Kind<F, ? extends A> value, Function1<? super A, ? extends B> mapper) {
    return foldLeft(value, monoid.zero(), (acc, a) -> monoid.combine(acc, mapper.apply(a)));
  }

  default <A> Option<A> reduce(Kind<F, ? extends A> value, Operator2<A> combinator) {
    return foldLeft(value, Option.none(),
        (option, a) -> option.fold(() -> Option.some(a), b -> Option.some(combinator.apply(b, a))));
  }

  default <G extends Witness, A, B> Kind<G, B> foldM(
      Monad<G> monad, Kind<F, ? extends A> value, B initial, 
      Function2<? super B, ? super A, ? extends Kind<G, ? extends B>> mapper) {
    return foldLeft(value, monad.pure(initial), (gb, a) -> monad.flatMap(gb, b -> mapper.apply(b, a)));
  }

  static <F extends Witness, G extends Witness> Foldable<Nested<F, G>> compose(Foldable<F> f, Foldable<G> g) {
    return new ComposedFoldable<>() {

      @Override
      public Foldable<F> f() { return f; }

      @Override
      public Foldable<G> g() { return g; }
    };
  }
}
