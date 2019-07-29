/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.TypeClass;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Option;

@TypeClass
public interface Foldable<F extends Kind> {

  <A, B> B foldLeft(Higher1<F, A> value, B initial, Function2<B, A, B> mapper);

  <A, B> Eval<B> foldRight(Higher1<F, A> value, Eval<B> initial, Function2<A, Eval<B>, Eval<B>> mapper);

  default <A> A fold(Monoid<A> monoid, Higher1<F, A> value) {
    return foldMap(monoid, value, identity());
  }

  default <A, B> B foldMap(Monoid<B> monoid, Higher1<F, A> value, Function1<A, B> mapper) {
    return foldLeft(value, monoid.zero(), (acc, a) -> monoid.combine(acc, mapper.apply(a)));
  }

  default <A> Option<A> reduce(Higher1<F, A> value, Operator2<A> combinator) {
    return foldLeft(value, Option.<A>none(),
        (option, a) -> option.fold(() -> Option.some(a), b -> Option.some(combinator.apply(b, a))));
  }

  default <G extends Kind, A, B> Higher1<G, B> foldM(Monad<G> monad, Higher1<F, A> value, B initial, Function2<B, A, Higher1<G, B>> mapper) {
    return foldLeft(value, monad.pure(initial), (gb, a) -> monad.flatMap(gb, b -> mapper.apply(b, a)));
  }

  static <F extends Kind, G extends Kind> Foldable<Nested<F, G>> compose(Foldable<F> f, Foldable<G> g) {
    return new ComposedFoldable<F, G>() {

      @Override
      public Foldable<F> f() { return f; }

      @Override
      public Foldable<G> g() { return g; }
    };
  }
}
