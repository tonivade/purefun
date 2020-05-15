/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;

public final class For2<F extends Kind, A, B> extends AbstractFor<F, A, B> {

  private final Producer<? extends Higher1<F, A>> value1;

  protected For2(Monad<F> monad,
                 Producer<? extends Higher1<F, A>> value1,
                 Function1<A, ? extends Higher1<F, B>> value2) {
    super(monad, value2);
    this.value1 = checkNonNull(value1);
  }

  public Higher1<F, Tuple2<A, B>> tuple() {
    return apply(Tuple2::of);
  }

  public <R> Higher1<F, R> apply(Function2<A, B, R> combinator) {
    Higher1<F, A> fa = value1.get();
    Higher1<F, B> fb = monad.flatMap(fa, value);
    return monad.map2(fa, fb, combinator);
  }

  public <R> Higher1<F, R> yield(Function2<A, B, R> combine) {
    return monad.flatMap(value1.get(),
        a -> monad.map(value.apply(a),
            b -> combine.apply(a, b)));
  }

  public <R> For3<F, A, B, R> map(Function1<B, R> mapper) {
    return flatMap(mapper.andThen(monad::<R>pure));
  }

  public <R> For3<F, A, B, R> and(R next) {
    return then(monad.pure(next));
  }

  public <R> For3<F, A, B, R> then(Higher1<F, R> next) {
    return andThen(cons(next));
  }

  public <R> For3<F, A, B, R> andThen(Producer<? extends Higher1<F, R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> For3<F, A, B, R> flatMap(Function1<B, ? extends Higher1<F, R>> mapper) {
    return new For3<>(monad, value1, value, mapper);
  }

  @Override
  public Higher1<F, B> run() {
    return yield((a, b) -> b);
  }
}
