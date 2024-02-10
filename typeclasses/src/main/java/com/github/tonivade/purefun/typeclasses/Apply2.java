/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public final class Apply2<F extends Witness, A, B> extends AbstractApply<F, B> {

  private final Producer<? extends Kind<F, ? extends A>> value1;

  Apply2(Applicative<F> applicative,
                 Producer<? extends Kind<F, ? extends A>> value1,
                 Producer<? extends Kind<F, ? extends B>> value2) {
    super(applicative, value2);
    this.value1 = checkNonNull(value1);
  }

  public Kind<F, Tuple2<A, B>> tuple() {
    return apply(Tuple2::of);
  }

  public <R> Kind<F, R> apply(Function2<? super A, ? super B, ? extends R> combinator) {
    Kind<F, ? extends A> fa = value1.get();
    Kind<F, ? extends B> fb = value.get();
    return applicative.mapN(fa, fb, combinator);
  }

  public <C> Apply3<F, A, B, C> and(C next) {
    return then(applicative.pure(next));
  }

  public <C> Apply3<F, A, B, C> then(Kind<F, ? extends C> next) {
    return andThen(cons(next));
  }

  public <C> Apply3<F, A, B, C> andThen(Producer<? extends Kind<F, ? extends C>> producer) {
    return new Apply3<>(applicative, value1, value, producer);
  }

  @Override
  public Kind<F, B> run() {
    return apply(Function2.second());
  }
}
