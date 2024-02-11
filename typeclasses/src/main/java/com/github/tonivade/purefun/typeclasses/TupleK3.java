/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import java.util.Objects;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.data.Sequence;

public class TupleK3<F extends Witness, A, B, C> implements TupleK<F> {
  
  private static final Equal<TupleK3<?, ?, ?, ?>> EQUAL = Equal.<TupleK3<?, ?, ?, ?>>of()
    .comparing(TupleK3::get1)
    .comparing(TupleK3::get2)
    .comparing(TupleK3::get3);
  
  private final Kind<F, A> value1;
  private final Kind<F, B> value2;
  private final Kind<F, C> value3;
  
  public TupleK3(Kind<F, A> value1, Kind<F, B> value2, Kind<F, C> value3) {
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
    this.value3 = checkNonNull(value3);
  }
  
  @Override
  public Sequence<Kind<F, ?>> toSequence() {
    return listOf(value1, value2, value3);
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

  @SuppressWarnings("unchecked")
  public <R> TupleK3<F, R, B, C> map1(Function1<? super A, ? extends R> mapper, F...reified) {
    return map1(Instances.functor(reified), mapper);
  }

  public <R> TupleK3<F, R, B, C> map1(Functor<F> functor, Function1<? super A, ? extends R> mapper) {
    return new TupleK3<>(functor.map(value1, mapper), value2, value3);
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK3<F, A, R, C> map2(Function1<? super B, ? extends R> mapper, F...reified) {
    return map2(Instances.functor(reified), mapper);
  }

  public <R> TupleK3<F, A, R, C> map2(Functor<F> functor, Function1<? super B, ? extends R> mapper) {
    return new TupleK3<>(value1, functor.map(value2, mapper), value3);
  }

  @SuppressWarnings("unchecked")
  public <R> TupleK3<F, A, B, R> map3(Function1<? super C, ? extends R> mapper, F...reified) {
    return map3(Instances.functor(reified), mapper);
  }

  public <R> TupleK3<F, A, B, R> map3(Functor<F> functor, Function1<? super C, ? extends R> mapper) {
    return new TupleK3<>(value1, value2, functor.map(value3, mapper));
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(value1, value2, value3);
  }
  
  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "TupleK3(" + value1 + "," + value2 + "," + value3 + ")";
  }
}
