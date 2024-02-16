/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Producer.cons;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple1;

public final class Apply1<F extends Witness, A> extends AbstractApply<F, A> {

  Apply1(Applicative<F> applicative, Producer<Kind<F, ? extends A>> value) {
    super(applicative, value);
  }

  public Kind<F, Tuple1<A>> tuple() {
    return apply(Tuple::of);
  }

  public <R> Kind<F, R> apply(Function1<? super A, ? extends R> combinator) {
    return applicative.map(value.get(), combinator);
  }

  public <B> Apply2<F, A, B> and(B next) {
    return then(applicative.pure(next));
  }

  public <B> Apply2<F, A, B> then(Kind<F, ? extends B> next) {
    return andThen(cons(next));
  }

  public <B> Apply2<F, A, B> andThen(Producer<? extends Kind<F, ? extends B>> producer) {
    return new Apply2<>(applicative, value, producer);
  }

  @Override
  public Kind<F, A> run() {
    return apply(identity());
  }
}
