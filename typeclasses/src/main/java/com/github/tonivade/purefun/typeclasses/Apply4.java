/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Function4.fourth;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple4;

public final class Apply4<F extends Witness, A, B, C, D> extends AbstractApply<F, D> {

  private final Producer<? extends Kind<F, ? extends A>> value1;
  private final Producer<? extends Kind<F, ? extends B>> value2;
  private final Producer<? extends Kind<F, ? extends C>> value3;

  Apply4(Applicative<F> applicative,
                 Producer<? extends Kind<F, ? extends A>> value1,
                 Producer<? extends Kind<F, ? extends B>> value2,
                 Producer<? extends Kind<F, ? extends C>> value3,
                 Producer<? extends Kind<F, ? extends D>> value4) {
    super(applicative, value4);
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
    this.value3 = checkNonNull(value3);
  }

  public Kind<F, Tuple4<A, B, C, D>> tuple() {
    return apply(Tuple4::of);
  }

  public <R> Kind<F, R> apply(Function4<? super A, ? super B, ? super C, ? super D, ? extends R> combine) {
    Kind<F, ? extends A> fa = value1.get();
    Kind<F, ? extends B> fb = value2.get();
    Kind<F, ? extends C> fc = value3.get();
    Kind<F, ? extends D> fd = value.get();
    return applicative.mapN(fa, fb, fc, fd, combine);
  }

  public <E> Apply5<F, A, B, C, D, E> and(E next) {
    return then(applicative.pure(next));
  }

  public <E> Apply5<F, A, B, C, D, E> then(Kind<F, E> next) {
    return andThen(cons(next));
  }

  public <E> Apply5<F, A, B, C, D, E> andThen(Producer<? extends Kind<F, E>> producer) {
    return new Apply5<>(applicative, value1, value2, value3, value, producer);
  }

  @Override
  public Kind<F, D> run() {
    return apply(fourth());
  }
}
