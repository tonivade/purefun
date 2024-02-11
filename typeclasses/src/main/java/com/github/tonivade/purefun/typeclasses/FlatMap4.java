/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Function4.fourth;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Producer.cons;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function4;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple4;

public final class FlatMap4<F extends Witness, A, B, C, D> extends AbstractFlatMap<F, C, D> {

  private final Producer<? extends Kind<F, ? extends A>> value1;
  private final Function1<? super A, ? extends Kind<F, ? extends B>> value2;
  private final Function1<? super B, ? extends Kind<F, ? extends C>> value3;

  FlatMap4(Monad<F> monad,
                 Producer<? extends Kind<F, ? extends A>> value1,
                 Function1<? super A, ? extends Kind<F, ? extends B>> value2,
                 Function1<? super B, ? extends Kind<F, ? extends C>> value3,
                 Function1<? super C, ? extends Kind<F, ? extends D>> value4) {
    super(monad, value4);
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
    this.value3 = checkNonNull(value3);
  }

  public Kind<F, Tuple4<A, B, C, D>> tuple() {
    return apply(Tuple4::of);
  }

  public <R> Kind<F, R> apply(Function4<A, B, C, D, R> combine) {
    return monad.flatMap(value1.get(),
        a -> monad.flatMap(value2.apply(a),
            b -> monad.flatMap(value3.apply(b),
                c -> monad.map(value.apply(c),
                    d -> combine.apply(a, b, c, d)))));
  }

  public <R> FlatMap5<F, A, B, C, D, R> map(Function1<D, R> mapper) {
    return flatMap(mapper.andThen(monad::<R>pure));
  }

  public <R> FlatMap5<F, A, B, C, D, R> and(R next) {
    return then(monad.pure(next));
  }

  public <R> FlatMap5<F, A, B, C, D, R> then(Kind<F, R> next) {
    return andThen(cons(next));
  }

  public <R> FlatMap5<F, A, B, C, D, R> andThen(Producer<? extends Kind<F, R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> FlatMap5<F, A, B, C, D, R> flatMap(Function1<D, ? extends Kind<F, R>> mapper) {
    return new FlatMap5<>(monad, value1, value2, value3, value, mapper);
  }

  @Override
  public Kind<F, D> run() {
    return apply(fourth());
  }
}
