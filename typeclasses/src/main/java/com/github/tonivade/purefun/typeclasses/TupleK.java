/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.data.Sequence;

public sealed interface TupleK<F extends Kind<F, ?>> permits TupleK1, TupleK2, TupleK3, TupleK4, TupleK5 {

  Sequence<Kind<F, ?>> toSequence();

  default void forEach(Consumer1<? super Kind<F, ?>> consumer) {
    toSequence().forEach(consumer);
  }

  static <F extends Kind<F, ?>, A> TupleK1<F, A> of(Kind<F, A> value1) {
    return new TupleK1<>(value1);
  }

  static <F extends Kind<F, ?>, A, B> TupleK2<F, A, B> of(Kind<F, A> value1, Kind<F, B> value2) {
    return new TupleK2<>(value1, value2);
  }

  static <F extends Kind<F, ?>, A, B, C> TupleK3<F, A, B, C> of(
      Kind<F, A> value1, Kind<F, B>  value2, Kind<F, C> value3) {
    return new TupleK3<>(value1, value2, value3);
  }

  static <F extends Kind<F, ?>, A, B, C, D> TupleK4<F, A, B, C, D> of(
      Kind<F, A> value1, Kind<F, B>  value2, Kind<F, C> value3, Kind<F, D> value4) {
    return new TupleK4<>(value1, value2, value3, value4);
  }

  static <F extends Kind<F, ?>, A, B, C, D, E> TupleK5<F, A, B, C, D, E> of(
      Kind<F, A> value1, Kind<F, B>  value2, Kind<F, C> value3, Kind<F, D> value4, Kind<F, E> value5) {
    return new TupleK5<>(value1, value2, value3, value4, value5);
  }
}
