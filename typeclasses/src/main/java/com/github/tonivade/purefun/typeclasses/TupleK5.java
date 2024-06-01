/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import java.util.Objects;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple5;
import com.github.tonivade.purefun.data.Sequence;

public final class TupleK5<F extends Kind<F, ?>, A, B, C, D, E> implements TupleK<F> {

  private static final Equal<TupleK5<?, ?, ?, ?, ?, ?>> EQUAL = Equal.<TupleK5<?, ?, ?, ?, ?, ?>>of()
    .comparing(TupleK5::get1)
    .comparing(TupleK5::get2)
    .comparing(TupleK5::get3)
    .comparing(TupleK5::get4)
    .comparing(TupleK5::get5);

  private final Kind<F, A> value1;
  private final Kind<F, B> value2;
  private final Kind<F, C> value3;
  private final Kind<F, D> value4;
  private final Kind<F, E> value5;

  public TupleK5(Kind<F, A> value1, Kind<F, B> value2, Kind<F, C> value3, Kind<F, D> value4, Kind<F, E> value5) {
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
    this.value3 = checkNonNull(value3);
    this.value4 = checkNonNull(value4);
    this.value5 = checkNonNull(value5);
  }

  @Override
  public Sequence<Kind<F, ?>> toSequence() {
    return listOf(value1, value2, value3, value4, value5);
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

  public Kind<F, E> get5() {
    return value5;
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK5<F, R, B, C, D, E> map1(Function1<? super A, ? extends R> mapper, F...reified) {
    return map1(Instances.functor(reified), mapper);
  }

  public <R> TupleK5<F, R, B, C, D, E> map1(Functor<F> functor, Function1<? super A, ? extends R> mapper) {
    return new TupleK5<>(functor.map(value1, mapper), value2, value3, value4, value5);
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK5<F, A, R, C, D, E> map2(Function1<? super B, ? extends R> mapper, F...reified) {
    return map2(Instances.functor(reified), mapper);
  }

  public <R> TupleK5<F, A, R, C, D, E> map2(Functor<F> functor, Function1<? super B, ? extends R> mapper) {
    return new TupleK5<>(value1, functor.map(value2, mapper), value3, value4, value5);
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK5<F, A, B, R, D, E> map3(Function1<? super C, ? extends R> mapper, F...reified) {
    return map3(Instances.functor(reified), mapper);
  }

  public <R> TupleK5<F, A, B, R, D, E> map3(Functor<F> functor, Function1<? super C, ? extends R> mapper) {
    return new TupleK5<>(value1, value2, functor.map(value3, mapper), value4, value5);
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK5<F, A, B, C, R, E> map4(Function1<? super D, ? extends R> mapper, F...reified) {
    return map4(Instances.functor(reified), mapper);
  }

  public <R> TupleK5<F, A, B, C, R, E> map4(Functor<F> functor, Function1<? super D, ? extends R> mapper) {
    return new TupleK5<>(value1, value2, value3, functor.map(value4, mapper), value5);
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK5<F, A, B, C, D, R> map5(Function1<? super E, ? extends R> mapper, F...reified) {
    return map5(Instances.functor(reified), mapper);
  }

  public <R> TupleK5<F, A, B, C, D, R> map5(Functor<F> functor, Function1<? super E, ? extends R> mapper) {
    return new TupleK5<>(value1, value2, value3, value4, functor.map(value5, mapper));
  }

  @SafeVarargs
  public final Kind<F, Tuple5<A, B, C, D, E>> apply(F...reified) {
    return apply(Instances.applicative(reified));
  }

  public Kind<F, Tuple5<A, B, C, D, E>> apply(Applicative<F> applicative) {
    return applicative.tuple(value1, value2, value3, value4, value5);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2, value3, value4, value5);
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "TupleK5(" + value1 + "," + value2 + "," + value3 + "," + value4 + "," + value5 + ")";
  }
}
