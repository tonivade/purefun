/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Producer.cons;
import static com.github.tonivade.purefun.core.Unit.unit;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple1;
import com.github.tonivade.purefun.core.Unit;

public final class FlatMap1<F extends Kind<F, ?>, A> extends AbstractFlatMap<F, Unit, A> {

  FlatMap1(Monad<F> monad, Producer<Kind<F, ? extends A>> value) {
    super(monad, value.asFunction());
  }

  public Kind<F, Tuple1<A>> tuple() {
    return monad.map(value.apply(unit()), Tuple1::of);
  }

  public <R> FlatMap2<F, A, R> map(Function1<? super A, ? extends R> mapper) {
    return flatMap(mapper.andThen(monad::<R>pure));
  }

  public <R> FlatMap2<F, A, R> and(R next) {
    return then(monad.pure(next));
  }

  public <R> FlatMap2<F, A, R> then(Kind<F, ? extends R> next) {
    return andThen(cons(next));
  }

  public <R> FlatMap2<F, A, R> andThen(Producer<? extends Kind<F, ? extends R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> FlatMap2<F, A, R> flatMap(Function1<? super A, ? extends Kind<F, ? extends R>> mapper) {
    return new FlatMap2<>(monad, () -> value.apply(unit()), mapper);
  }

  @Override
  public Kind<F, A> run() {
    return Kind.narrowK(value.apply(unit()));
  }
}
