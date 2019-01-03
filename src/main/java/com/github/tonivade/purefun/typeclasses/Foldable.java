/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nested.unnest;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.type.Option;

public interface Foldable<F extends Kind> {

  <A, B> B foldLeft(Higher1<F, A> value, B initial, Function2<B, A, B> mapper);

  <A, B> B foldRight(Higher1<F, A> value, B initial, Function2<A, B, B> mapper);

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

  static <F extends Kind, G extends Kind> Foldable<Nested<F, G>> compose(Foldable<F> ff, Foldable<G> fg) {
    return new Foldable<Nested<F, G>>() {

      @Override
      public <A, B> B foldLeft(Higher1<Nested<F, G>, A> value, B initial, Function2<B, A, B> mapper) {
        return ff.foldLeft(unnest(value), initial, (a, b) -> fg.foldLeft(b, a, mapper));
      }

      @Override
      public <A, B> B foldRight(Higher1<Nested<F, G>, A> value, B initial, Function2<A, B, B> mapper) {
        return ff.foldRight(unnest(value), initial, (a, b) -> fg.foldRight(a, b, mapper));
      }
    };
  }
}
