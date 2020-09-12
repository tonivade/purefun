/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple3;

public final class For3<F extends Witness, A, B, C> extends AbstractFor<F, B, C> {

  private final Producer<? extends Kind<F, A>> value1;
  private final Function1<A, ? extends Kind<F, B>> value2;

  protected For3(Monad<F> monad,
                 Producer<? extends Kind<F, A>> value1,
                 Function1<A, ? extends Kind<F, B>> value2,
                 Function1<B, ? extends Kind<F, C>> value3) {
    super(monad, value3);
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
  }

  public Kind<F, Tuple3<A, B, C>> tuple() {
    return apply(Tuple3::of);
  }

  public <R> Kind<F, R> apply(Function3<A, B, C, R> combine) {
    Kind<F, A> fa = value1.get();
    Kind<F, B> fb = monad.flatMap(fa, value2);
    Kind<F, C> fc = monad.flatMap(fb, value);
    return monad.mapN(fa, fb, fc, combine);
  }

  public <R> Kind<F, R> yield(Function3<A, B, C, R> combine) {
    return monad.flatMap(value1.get(),
        a -> monad.flatMap(value2.apply(a),
            b -> monad.map(value.apply(b),
                c -> combine.apply(a, b, c))));
  }

  public <R> For4<F, A, B, C, R> map(Function1<C, R> mapper) {
    return flatMap(mapper.andThen(monad::<R>pure));
  }

  public <R> For4<F, A, B, C, R> and(R next) {
    return then(monad.pure(next));
  }

  public <R> For4<F, A, B, C, R> then(Kind<F, R> next) {
    return andThen(cons(next));
  }

  public <R> For4<F, A, B, C, R> andThen(Producer<? extends Kind<F, R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> For4<F, A, B, C, R> flatMap(Function1<C, ? extends Kind<F, R>> mapper) {
    return new For4<>(monad, value1, value2, value, mapper);
  }

  @Override
  public Kind<F, C> run() {
    return this.yield((a, b, c) -> c);
  }
}
