/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import java.util.Objects;
import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.data.Sequence;

public class TupleK2<F extends Witness, A, B> implements TupleK<F> {
  
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

  public <R> TupleK2<F, R, B> map1(Functor<F> functor, Function1<? super A, ? extends R> mapper) {
    return new TupleK2<>(functor.map(value1, mapper), value2);
  }

  public <R> TupleK2<F, A, R> map2(Functor<F> functor, Function1<? super B, ? extends R> mapper) {
    return new TupleK2<>(value1, functor.map(value2, mapper));
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
