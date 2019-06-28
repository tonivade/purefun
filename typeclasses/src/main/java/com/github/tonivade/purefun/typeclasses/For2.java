/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;

public final class For2<F extends Kind, A, B> {

  private final Monad<F> monad;
  private final Higher1<F, A> value1;
  private final Higher1<F, B> value2;

  protected For2(Monad<F> monad, Higher1<F, A> value1, Higher1<F, B> value2) {
    this.monad = requireNonNull(monad);
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
  }

  public Higher1<F, Tuple2<A, B>> get() {
    return yield(Tuple2::of);
  }

  public <R> Higher1<F, R> yield(Function2<A, B, R> combinator) {
    return monad.map2(value1, value2, combinator);
  }

  public <R> R fix(Function1<Higher1<F, B>, R> mapper) {
    return mapper.apply(value2);
  }

  public void end(Consumer1<Higher1<F, B>> consumer) {
     consumer.accept(value2);
  }

  public <R> Higher1<F, R> returns(R value) {
    return monad.pure(value);
  }

  public <R> For3<F, A, B, R> map(Function1<B, R> mapper) {
    return For.with(monad, value1, value2, monad.map(value2, mapper));
  }

  public <R> For3<F, A, B, R> andThen(Producer<Higher1<F, R>> producer) {
    return For.with(monad, value1, value2, monad.flatMap(value2, i -> producer.get()));
  }

  public <R> For3<F, A, B, R> flatMap(Function1<B, ? extends Higher1<F, R>> mapper) {
    return For.with(monad, value1, value2, monad.flatMap(value2, mapper));
  }
}
