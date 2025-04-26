/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import java.util.Objects;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.data.Sequence;

public final class TupleK2<F extends Kind<F, ?>, A, B> implements TupleK<F> {

  private static final Equal<TupleK2<?, ?, ?>> EQUAL = Equal.<TupleK2<?, ?, ?>>of()
    .comparing(TupleK2::get1)
    .comparing(TupleK2::get2);

  private final Kind<F, A> value1;
  private final Kind<F, B> value2;

  public TupleK2(Kind<F, A> value1, Kind<F, B> value2) {
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
  }

  @Override
  public Sequence<Kind<F, ?>> toSequence() {
    return listOf(value1, value2);
  }

  public Kind<F, A> get1() {
    return value1;
  }

  public Kind<F, B> get2() {
    return value2;
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK2<F, R, B> map1(Function1<? super A, ? extends R> mapper, F...reified) {
    return map1(Instances.functor(reified), mapper);
  }

  public <R> TupleK2<F, R, B> map1(Functor<F> functor, Function1<? super A, ? extends R> mapper) {
    return new TupleK2<>(functor.map(value1, mapper), value2);
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK2<F, A, R> map2(Function1<? super B, ? extends R> mapper, F...reified) {
    return map2(Instances.functor(reified), mapper);
  }

  public <R> TupleK2<F, A, R> map2(Functor<F> functor, Function1<? super B, ? extends R> mapper) {
    return new TupleK2<>(value1, functor.map(value2, mapper));
  }

  @SafeVarargs
  public final Kind<F, Tuple2<A, B>> apply(F...reified) {
    return apply(Instances.applicative(reified));
  }

  public Kind<F, Tuple2<A, B>> apply(Applicative<F> applicative) {
    return applicative.tuple(value1, value2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2);
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "TupleK2(" + value1 + "," + value2 + ")";
  }
}
