/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import java.util.Objects;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple4;
import com.github.tonivade.purefun.data.Sequence;

public final class TupleK4<F extends Kind<F, ?>, A, B, C, D> implements TupleK<F> {

  private static final Equal<TupleK4<?, ?, ?, ?, ?>> EQUAL = Equal.<TupleK4<?, ?, ?, ?, ?>>of()
    .comparing(TupleK4::get1)
    .comparing(TupleK4::get2)
    .comparing(TupleK4::get3)
    .comparing(TupleK4::get4);

  private final Kind<F, A> value1;
  private final Kind<F, B> value2;
  private final Kind<F, C> value3;
  private final Kind<F, D> value4;

  public TupleK4(Kind<F, A> value1, Kind<F, B> value2, Kind<F, C> value3, Kind<F, D> value4) {
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
    this.value3 = checkNonNull(value3);
    this.value4 = checkNonNull(value4);
  }

  @Override
  public Sequence<Kind<F, ?>> toSequence() {
    return listOf(value1, value2, value3, value4);
  }

  public Kind<F, A> get1() {
    return value1;
  }

  public Kind<F, B> get2() {
    return value2;
  }

  public Kind<F, C> get3() {
    return value3;
  }

  public Kind<F, D> get4() {
    return value4;
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK4<F, R, B, C, D> map1(Function1<? super A, ? extends R> mapper, F...reified) {
    return map1(Instances.functor(reified), mapper);
  }

  public <R> TupleK4<F, R, B, C, D> map1(Functor<F> functor, Function1<? super A, ? extends R> mapper) {
    return new TupleK4<>(functor.map(value1, mapper), value2, value3, value4);
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK4<F, A, R, C, D> map2(Function1<? super B, ? extends R> mapper, F...reified) {
    return map2(Instances.functor(reified), mapper);
  }

  public <R> TupleK4<F, A, R, C, D> map2(Functor<F> functor, Function1<? super B, ? extends R> mapper) {
    return new TupleK4<>(value1, functor.map(value2, mapper), value3, value4);
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK4<F, A, B, R, D> map3(Function1<? super C, ? extends R> mapper, F...reified) {
    return map3(Instances.functor(reified), mapper);
  }

  public <R> TupleK4<F, A, B, R, D> map3(Functor<F> functor, Function1<? super C, ? extends R> mapper) {
    return new TupleK4<>(value1, value2, functor.map(value3, mapper), value4);
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK4<F, A, B, C, R> map4(Function1<? super D, ? extends R> mapper, F...reified) {
    return map4(Instances.functor(reified), mapper);
  }

  public <R> TupleK4<F, A, B, C, R> map4(Functor<F> functor, Function1<? super D, ? extends R> mapper) {
    return new TupleK4<>(value1, value2, value3, functor.map(value4, mapper));
  }

  @SafeVarargs
  public final Kind<F, Tuple4<A, B, C, D>> apply(F...reified) {
    return apply(Instances.applicative(reified));
  }

  public Kind<F, Tuple4<A, B, C, D>> apply(Applicative<F> applicative) {
    return applicative.tuple(value1, value2, value3, value4);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2, value3, value4);
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "TupleK4(" + value1 + "," + value2 + "," + value3 + "," + value4 + ")";
  }
}
