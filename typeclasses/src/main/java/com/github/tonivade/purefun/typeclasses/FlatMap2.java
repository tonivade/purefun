/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Function2.second;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;

public final class FlatMap2<F extends Witness, A, B> extends AbstractFlatMap<F, A, B> {

  private final Producer<? extends Kind<F, ? extends A>> value1;

  FlatMap2(Monad<F> monad,
                 Producer<? extends Kind<F, ? extends A>> value1,
                 Function1<? super A, ? extends Kind<F, ? extends B>> value2) {
    super(monad, value2);
    this.value1 = checkNonNull(value1);
  }

  public Kind<F, Tuple2<A, B>> tuple() {
    return apply(Tuple2::of);
  }

  @Deprecated
  public <R> Kind<F, R> yield(Function2<? super A, ? super B, ? extends R> combine) {
    return apply(combine);
  }

  public <R> Kind<F, R> apply(Function2<? super A, ? super B, ? extends R> combine) {
    return monad.flatMap(value1.get(),
        a -> monad.map(value.apply(a),
            b -> combine.apply(a, b)));
  }

  public <R> FlatMap3<F, A, B, R> map(Function1<? super B, ? extends R> mapper) {
    return flatMap(mapper.andThen(monad::<R>pure));
  }

  public <R> FlatMap3<F, A, B, R> and(R next) {
    return then(monad.pure(next));
  }

  public <R> FlatMap3<F, A, B, R> then(Kind<F, ? extends R> next) {
    return andThen(cons(next));
  }

  public <R> FlatMap3<F, A, B, R> andThen(Producer<? extends Kind<F, ? extends R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> FlatMap3<F, A, B, R> flatMap(Function1<? super B, ? extends Kind<F, ? extends R>> mapper) {
    return new FlatMap3<>(monad, value1, value, mapper);
  }

  @Override
  public Kind<F, B> run() {
    return apply(second());
  }
}
