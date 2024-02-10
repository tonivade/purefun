/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;
import com.github.tonivade.purefun.data.Sequence;

public interface TupleK<F extends Witness> {

  Sequence<Kind<F, ?>> toSequence();

  default void forEach(Consumer1<? super Kind<F, ?>> consumer) {
    toSequence().forEach(consumer::accept);
  }
  
  static <F extends Witness, A> TupleK1<F, A> of(Kind<F, A> value1) {
    return new TupleK1<>(value1);
  }

  static <F extends Witness, A, B> TupleK2<F, A, B> of(Kind<F, A> value1, Kind<F, B> value2) {
    return new TupleK2<>(value1, value2);
  }

  static <F extends Witness, A, B, C> TupleK3<F, A, B, C> of(
      Kind<F, A> value1, Kind<F, B>  value2, Kind<F, C> value3) {
    return new TupleK3<>(value1, value2, value3);
  }

  static <F extends Witness, A, B, C, D> TupleK4<F, A, B, C, D> of(
      Kind<F, A> value1, Kind<F, B>  value2, Kind<F, C> value3, Kind<F, D> value4) {
    return new TupleK4<>(value1, value2, value3, value4);
  }

  static <F extends Witness, A, B, C, D, E> TupleK5<F, A, B, C, D, E> of(
      Kind<F, A> value1, Kind<F, B>  value2, Kind<F, C> value3, Kind<F, D> value4, Kind<F, E> value5) {
    return new TupleK5<>(value1, value2, value3, value4, value5);
  }
}
