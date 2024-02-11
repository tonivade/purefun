/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Function5.fifth;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function5;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple5;

public final class Apply5<F extends Witness, A, B, C, D, E> extends AbstractApply<F, E> {

  private final Producer<? extends Kind<F, ? extends A>> value1;
  private final Producer<? extends Kind<F, ? extends B>> value2;
  private final Producer<? extends Kind<F, ? extends C>> value3;
  private final Producer<? extends Kind<F, ? extends D>> value4;

  Apply5(Applicative<F> applicative,
                 Producer<? extends Kind<F, ? extends A>> value1,
                 Producer<? extends Kind<F, ? extends B>> value2,
                 Producer<? extends Kind<F, ? extends C>> value3,
                 Producer<? extends Kind<F, ? extends D>> value4,
                 Producer<? extends Kind<F, ? extends E>> value5) {
    super(applicative, value5);
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
    this.value3 = checkNonNull(value3);
    this.value4 = checkNonNull(value4);
  }

  public Kind<F, Tuple5<A, B, C, D, E>> tuple() {
    return apply(Tuple5::of);
  }

  public <R> Kind<F, R> apply(Function5<A, B, C, D, E, R> combine) {
    Kind<F, ? extends A> fa = value1.get();
    Kind<F, ? extends B> fb = value2.get();
    Kind<F, ? extends C> fc = value3.get();
    Kind<F, ? extends D> fd = value4.get();
    Kind<F, ? extends E> fe = value.get();
    return applicative.mapN(fa, fb, fc, fd, fe, combine);
  }

  @Override
  public Kind<F, E> run() {
    return this.apply(fifth());
  }
}
