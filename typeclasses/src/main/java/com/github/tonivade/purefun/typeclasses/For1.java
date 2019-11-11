/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple1;
import com.github.tonivade.purefun.Unit;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Unit.unit;

public final class For1<F extends Kind, A> extends AbstractFor<F, Unit, A> {

  protected For1(Monad<F> monad, Producer<Higher1<F, A>> value) {
    super(monad, value.asFunction());
  }

  public Higher1<F, Tuple1<A>> tuple() {
    return apply(Tuple::of);
  }

  public <R> Higher1<F, R> apply(Function1<A, R> combinator) {
    return monad.map(value.apply(unit()), combinator);
  }

  public <R> For2<F, A, R> map(Function1<A, R> mapper) {
    return flatMap(mapper.andThen(monad::<R>pure));
  }

  public <R> For2<F, A, R> and(R next) {
    return and(monad.pure(next));
  }

  public <R> For2<F, A, R> and(Higher1<F, R> next) {
    return andThen(cons(next));
  }

  public <R> For2<F, A, R> andThen(Producer<? extends Higher1<F, R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> For2<F, A, R> flatMap(Function1<A, ? extends Higher1<F, R>> mapper) {
    return new For2<>(monad, () -> value.apply(unit()), mapper);
  }

  @Override
  public Higher1<F, A> get() {
    return apply(identity());
  }
}
