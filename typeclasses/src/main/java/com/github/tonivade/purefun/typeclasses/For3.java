/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple3;

public final class For3<F extends Kind, A, B, C> {

  private final Monad<F> monad;
  private final Higher1<F, A> value1;
  private final Higher1<F, B> value2;
  private final Higher1<F, C> value3;

  protected For3(Monad<F> monad,
                 Higher1<F, A> value1,
                 Higher1<F, B> value2,
                 Higher1<F, C> value3) {
    this.monad = requireNonNull(monad);
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
    this.value3 = requireNonNull(value3);
  }

  public Higher1<F, Tuple3<A, B, C>> get() {
    return yield(Tuple3::of);
  }

  public <R> Higher1<F, R> yield(Function3<A, B, C, R> combine) {
    return monad.map3(value1, value2, value3, combine);
  }

  public <R> R fix(Function1<Higher1<F, C>, R> mapper) {
    return mapper.apply(value3);
  }

  public void end(Consumer1<Higher1<F, C>> consumer) {
     consumer.accept(value3);
  }

  public <R> Higher1<F, R> returns(R value) {
    return monad.pure(value);
  }

  public <R> For4<F, A, B, C, R> map(Function1<C, R> mapper) {
    return For.with(monad, value1, value2, value3, monad.map(value3, mapper));
  }

  public <R> For4<F, A, B, C, R> andThen(Producer<Higher1<F, R>> producer) {
    return For.with(monad, value1, value2, value3, monad.flatMap(value3, i -> producer.get()));
  }

  public <R> For4<F, A, B, C, R> flatMap(Function1<C, ? extends Higher1<F, R>> mapper) {
    return For.with(monad, value1, value2, value3, monad.flatMap(value3, mapper));
  }
}
