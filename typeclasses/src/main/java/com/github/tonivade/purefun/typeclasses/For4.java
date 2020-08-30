/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple4;

public final class For4<F extends Witness, A, B, C, D> extends AbstractFor<F, C, D> {

  private final Producer<? extends Kind<F, A>> value1;
  private final Function1<A, ? extends Kind<F, B>> value2;
  private final Function1<B, ? extends Kind<F, C>> value3;

  protected For4(Monad<F> monad,
                 Producer<? extends Kind<F, A>> value1,
                 Function1<A, ? extends Kind<F, B>> value2,
                 Function1<B, ? extends Kind<F, C>> value3,
                 Function1<C, ? extends Kind<F, D>> value4) {
    super(monad, value4);
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
    this.value3 = checkNonNull(value3);
  }

  public Kind<F, Tuple4<A, B, C, D>> tuple() {
    return apply(Tuple4::of);
  }

  public <R> Kind<F, R> apply(Function4<A, B, C, D, R> combine) {
    Kind<F, A> fa = value1.get();
    Kind<F, B> fb = monad.flatMap(fa, value2);
    Kind<F, C> fc = monad.flatMap(fb, value3);
    Kind<F, D> fd = monad.flatMap(fc, value);
    return monad.map4(fa, fb, fc, fd, combine);
  }

  public <R> Kind<F, R> yield(Function4<A, B, C, D, R> combine) {
    return monad.flatMap(value1.get(),
        a -> monad.flatMap(value2.apply(a),
            b -> monad.flatMap(value3.apply(b),
                c -> monad.map(value.apply(c),
                    d -> combine.apply(a, b, c, d)))));
  }

  public <R> For5<F, A, B, C, D, R> map(Function1<D, R> mapper) {
    return flatMap(mapper.andThen(monad::<R>pure));
  }

  public <R> For5<F, A, B, C, D, R> and(R next) {
    return then(monad.pure(next));
  }

  public <R> For5<F, A, B, C, D, R> then(Kind<F, R> next) {
    return andThen(cons(next));
  }

  public <R> For5<F, A, B, C, D, R> andThen(Producer<? extends Kind<F, R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> For5<F, A, B, C, D, R> flatMap(Function1<D, ? extends Kind<F, R>> mapper) {
    return new For5<>(monad, value1, value2, value3, value, mapper);
  }

  @Override
  public Kind<F, D> run() {
    return this.yield((a, b, c, d) -> d);
  }
}
