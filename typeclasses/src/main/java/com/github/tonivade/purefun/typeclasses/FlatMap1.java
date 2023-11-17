/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Unit.unit;

public final class FlatMap1<F extends Witness, A> extends AbstractFlatMap<F, Unit, A> {

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
    return value.apply(unit()).fix(Kind::narrowK);
  }
}
