/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.core.Tuple1;
import com.github.tonivade.purefun.data.Sequence;

public final class TupleK1<F extends Witness, A> implements TupleK<F> {

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

  @SafeVarargs
  public final Kind<F, Tuple1<A>> apply(F...reified) {
    return apply(Instances.applicative(reified));
  }

  public Kind<F, Tuple1<A>> apply(Applicative<F> applicative) {
    return applicative.ap(value1, applicative.pure(Tuple1::of));
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
