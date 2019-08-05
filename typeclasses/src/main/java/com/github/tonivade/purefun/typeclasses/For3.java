/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple3;

public final class For3<F extends Kind, A, B, C> extends AbstractFor<F, C> {

  private final Higher1<F, A> value1;
  private final Higher1<F, B> value2;

  protected For3(Monad<F> monad,
                 Higher1<F, A> value1,
                 Higher1<F, B> value2,
                 Higher1<F, C> value3) {
    super(monad, value3);
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
  }

  public Higher1<F, Tuple3<A, B, C>> tuple() {
    return yield(Tuple3::of);
  }

  public <R> Higher1<F, R> yield(Function3<A, B, C, R> combine) {
    return monad.map3(value1, value2, value, combine);
  }

  public <R> For4<F, A, B, C, R> map(Function1<C, R> mapper) {
    return For.with(monad, value1, value2, value, monad.map(value, mapper));
  }

  public <R> For4<F, A, B, C, R> and(Higher1<F, R> next) {
    return For.with(monad, value1, value2, value, next);
  }

  public <R> For4<F, A, B, C, R> andThen(Producer<Higher1<F, R>> producer) {
    return For.with(monad, value1, value2, value, monad.andThen(value, producer));
  }

  public <R> For4<F, A, B, C, R> flatMap(Function1<C, ? extends Higher1<F, R>> mapper) {
    return For.with(monad, value1, value2, value, monad.flatMap(value, mapper));
  }
}
