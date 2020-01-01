/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Producer.cons;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple4;

public final class For4<F extends Kind, A, B, C, D> extends AbstractFor<F, C, D> {

  private final Producer<? extends Higher1<F, A>> value1;
  private final Function1<A, ? extends Higher1<F, B>> value2;
  private final Function1<B, ? extends Higher1<F, C>> value3;

  protected For4(Monad<F> monad,
                 Producer<? extends Higher1<F, A>> value1,
                 Function1<A, ? extends Higher1<F, B>> value2,
                 Function1<B, ? extends Higher1<F, C>> value3,
                 Function1<C, ? extends Higher1<F, D>> value4) {
    super(monad, value4);
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
    this.value3 = requireNonNull(value3);
  }

  public Higher1<F, Tuple4<A, B, C, D>> tuple() {
    return apply(Tuple4::of);
  }

  public <R> Higher1<F, R> apply(Function4<A, B, C, D, R> combine) {
    Higher1<F, A> fa = value1.get();
    Higher1<F, B> fb = monad.flatMap(fa, value2);
    Higher1<F, C> fc = monad.flatMap(fb, value3);
    Higher1<F, D> fd = monad.flatMap(fc, value);
    return monad.map4(fa, fb, fc, fd, combine);
  }

  public <R> Higher1<F, R> yield(Function4<A, B, C, D, R> combine) {
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
    return and(monad.pure(next));
  }

  public <R> For5<F, A, B, C, D, R> and(Higher1<F, R> next) {
    return andThen(cons(next));
  }

  public <R> For5<F, A, B, C, D, R> andThen(Producer<? extends Higher1<F, R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> For5<F, A, B, C, D, R> flatMap(Function1<D, ? extends Higher1<F, R>> mapper) {
    return new For5<>(monad, value1, value2, value3, value, mapper);
  }

  @Override
  public Higher1<F, D> run() {
    return yield((a, b, c, d) -> d);
  }
}
