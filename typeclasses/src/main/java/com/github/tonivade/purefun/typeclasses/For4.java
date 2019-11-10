/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Producer.*;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple4;

public final class For4<F extends Kind, A, B, C, D> extends AbstractFor<F, D> {

  private final Higher1<F, A> value1;
  private final Higher1<F, B> value2;
  private final Higher1<F, C> value3;

  protected For4(Monad<F> monad,
                 Higher1<F, A> value1,
                 Higher1<F, B> value2,
                 Higher1<F, C> value3,
                 Higher1<F, D> value4) {
    super(monad, value4);
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
    this.value3 = requireNonNull(value3);
  }

  public Higher1<F, Tuple4<A, B, C, D>> tuple() {
    return apply(Tuple4::of);
  }

  public <R> Higher1<F, R> apply(Function4<A, B, C, D, R> combine) {
    return monad.map4(value1, value2, value3, value, combine);
  }

  public <R> For5<F, A, B, C, D, R> map(Function1<D, R> mapper) {
    return flatMap(mapper.andThen(monad::<R>pure));
  }

  public <R> For5<F, A, B, C, D, R> and(Higher1<F, R> next) {
    return andThen(cons(next));
  }

  public <R> For5<F, A, B, C, D, R> andThen(Producer<Higher1<F, R>> producer) {
    return flatMap(producer.asFunction());
  }

  public <R> For5<F, A, B, C, D, R> flatMap(Function1<D, ? extends Higher1<F, R>> mapper) {
    return For.with(monad, value1, value2, value3, value, monad.flatMap(value, mapper));
  }
}
