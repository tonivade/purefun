/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.generic;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.generic.HList.HCons;

@FunctionalInterface
public interface HApply<F, A, R> {

  R apply(F context, A value);

  static <A, B> HApply<Function1<A, B>, A, B> function() {
    return (function, value) -> function.apply(value);
  }

  static <F, A> HApply<F, A, A> identity() {
    return (context, value) -> value;
  }

  static <F, A, B, C> HApply<F, Tuple2<Function1<A, B>, Function1<B, C>>, Function1<A, C>> compose() {
    return combine(Function1::andThen);
  }

  static <F, A, B, C> HApply<F, Tuple2<A, B>, C> combine(Function2<A, B, C> combinator) {
    return (context, tuple) -> tuple.applyTo(combinator);
  }

  static <F, E, R, L extends HList<L>> HApply<F, Tuple2<E, L>, HCons<R, L>> cons(Function1<E, R> mapper) {
    return (context, tuple) -> tuple.map1(mapper).applyTo(HList::cons);
  }
}
