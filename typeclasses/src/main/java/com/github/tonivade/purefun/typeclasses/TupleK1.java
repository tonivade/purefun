/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import java.util.Objects;
import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;
import com.github.tonivade.purefun.data.Sequence;

public class TupleK1<F extends Witness, A> implements TupleK<F> {

  private static final Equal<TupleK1<?, ?>> EQUAL = Equal.<TupleK1<?, ?>>of().comparing(TupleK1::get1);
  
  private final Kind<F, A> value1;
  
  public TupleK1(Kind<F, A> value1) {
    this.value1 = checkNonNull(value1);
  }
  
  @Override
  public Sequence<Kind<F, ?>> toSequence() {
    return listOf(value1);
  }

  public Kind<F, A> get1() {
    return value1;
  }

  @SuppressWarnings("unchecked")
  public <B> TupleK1<F, B> map1(Function1<? super A, ? extends B> mapper, F...reified) {
    return map1(Instances.functor(reified), mapper);
  }

  public <B> TupleK1<F, B> map1(Functor<F> functor, Function1<? super A, ? extends B> mapper) {
    return new TupleK1<>(functor.map(value1, mapper));
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(value1);
  }
  
  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "TupleK1(" + value1 + ")";
  }
}
