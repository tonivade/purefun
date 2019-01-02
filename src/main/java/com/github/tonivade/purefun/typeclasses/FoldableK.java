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

public interface FoldableK<F extends Kind> {

  <A, B> B foldLeft(Higher1<F, A> value, B initial, Function2<B, A, B> mapper);

  <A, B> B foldRight(Higher1<F, A> value, B initial, Function2<A, B, B> mapper);

  default <A> A fold(Monoid<A> monoid, Higher1<F, A> value) {
    return foldMap(monoid, value, identity());
  }

  default <A, B> B foldMap(Monoid<B> monoid, Higher1<F, A> value, Function1<A, B> mapper) {
    return foldLeft(value, monoid.zero(), (acc, a) -> monoid.combine(acc, mapper.apply(a)));
  }
}
