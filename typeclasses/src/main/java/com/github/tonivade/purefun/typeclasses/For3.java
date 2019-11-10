/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Producer.cons;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple3;

public final class For3<F extends Kind, A, B, C> extends AbstractFor<F, B, C> {

  private final Producer<? extends Higher1<F, A>> value1;
  private final Function1<A, ? extends Higher1<F, B>> value2;

  protected For3(Monad<F> monad,
                 Producer<? extends Higher1<F, A>> value1,
                 Function1<A, ? extends Higher1<F, B>> value2,
                 Function1<B, ? extends Higher1<F, C>> value3) {
    super(monad, value3);
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
  }

  public Higher1<F, Tuple3<A, B, C>> tuple() {
    return apply(Tuple3::of);
  }

  public <R> Higher1<F, R> apply(Function3<A, B, C, R> combine) {
    Higher1<F, A> fa = value1.get();
    Higher1<F, B> fb = monad.flatMap(fa, value2);
    Higher1<F, C> fc = monad.flatMap(fb, value);
    return monad.map3(fa, fb, fc, combine);
  }

  public <R> Higher1<F, R> yield(Function3<A, B, C, R> combine) {
    return monad.flatMap(value1.get(),
        a -> monad.flatMap(value2.apply(a),
            b -> monad.map(value.apply(b),
                c -> combine.apply(a, b, c))));
  }

  public <R> For4<F, A, B, C, R> map(Function1<C, R> mapper) {
    return flatMap(mapper.andThen(monad::<R>pure));
  }

  public <R> For4<F, A, B, C, R> and(Higher1<F, R> next) {
    return andThen(cons(next));
  }

  public <R> For4<F, A, B, C, R> andThen(Producer<Higher1<F, R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> For4<F, A, B, C, R> flatMap(Function1<C, ? extends Higher1<F, R>> mapper) {
    return new For4<>(monad, value1, value2, value, mapper);
  }

  @Override
  public Higher1<F, C> get() {
    return yield((a, b, c) -> c);
  }
}
