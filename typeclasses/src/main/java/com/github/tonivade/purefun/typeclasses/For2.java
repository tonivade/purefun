/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;

public final class For2<F extends Kind, A, B> extends AbstractFor<F, B> {

  private final Higher1<F, A> value1;

  protected For2(Monad<F> monad, Higher1<F, A> value1, Higher1<F, B> value2) {
    super(monad, value2);
    this.value1 = requireNonNull(value1);
  }

  public Higher1<F, Tuple2<A, B>> tuple() {
    return yield(Tuple2::of);
  }

  public <R> Higher1<F, R> yield(Function2<A, B, R> combinator) {
    return monad.map2(value1, value, combinator);
  }

  public <R> For3<F, A, B, R> map(Function1<B, R> mapper) {
    return For.with(monad, value1, value, monad.map(value, mapper));
  }

  public <R> For3<F, A, B, R> andThen(Producer<Higher1<F, R>> producer) {
    return For.with(monad, value1, value, monad.flatMap(value, i -> producer.get()));
  }

  public <R> For3<F, A, B, R> flatMap(Function1<B, ? extends Higher1<F, R>> mapper) {
    return For.with(monad, value1, value, monad.flatMap(value, mapper));
  }
}
