/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple1;
import com.github.tonivade.purefun.Unit;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Unit.unit;

public final class For1<F extends Witness, A> extends AbstractFor<F, Unit, A> {

  protected For1(Monad<F> monad, Producer<Kind<F, ? extends A>> value) {
    super(monad, value.asFunction());
  }

  public Kind<F, Tuple1<A>> tuple() {
    return apply(Tuple::of);
  }

  public <R> Kind<F, R> apply(Function1<? super A, ? extends R> combinator) {
    return monad.map(value.apply(unit()), combinator);
  }

  public <R> For2<F, A, R> map(Function1<? super A, ? extends R> mapper) {
    return flatMap(mapper.andThen(monad::<R>pure));
  }

  public <R> For2<F, A, R> and(R next) {
    return then(monad.pure(next));
  }

  public <R> For2<F, A, R> then(Kind<F, ? extends R> next) {
    return andThen(cons(next));
  }

  public <R> For2<F, A, R> andThen(Producer<? extends Kind<F, ? extends R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> For2<F, A, R> flatMap(Function1<? super A, ? extends Kind<F, ? extends R>> mapper) {
    return new For2<>(monad, () -> value.apply(unit()), mapper);
  }

  @Override
  public Kind<F, A> run() {
    return apply(identity());
  }
}
