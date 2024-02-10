/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function3.third;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Producer.cons;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple3;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public final class Apply3<F extends Witness, A, B, C> extends AbstractApply<F, C> {

  private final Producer<? extends Kind<F, ? extends A>> value1;
  private final Producer<? extends Kind<F, ? extends B>> value2;

  Apply3(Applicative<F> applicative,
                 Producer<? extends Kind<F, ? extends A>> value1,
                 Producer<? extends Kind<F, ? extends B>> value2,
                 Producer<? extends Kind<F, ? extends C>> value3) {
    super(applicative, value3);
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
  }

  public Kind<F, Tuple3<A, B, C>> tuple() {
    return apply(Tuple3::of);
  }

  public <R> Kind<F, R> apply(Function3<? super A, ? super B, ? super C, ? extends R> combine) {
    Kind<F, ? extends A> fa = value1.get();
    Kind<F, ? extends B> fb = value2.get();
    Kind<F, ? extends C> fc = value.get();
    return applicative.mapN(fa, fb, fc, combine);
  }

  public <D> Apply4<F, A, B, C, D> and(D next) {
    return then(applicative.pure(next));
  }

  public <D> Apply4<F, A, B, C, D> then(Kind<F, ? extends D> next) {
    return andThen(cons(next));
  }

  public <D> Apply4<F, A, B, C, D> andThen(Producer<? extends Kind<F, ? extends D>> producer) {
    return new Apply4<>(applicative, value1, value2, value, producer);
  }

  @Override
  public Kind<F, C> run() {
    return apply(third());
  }
}
