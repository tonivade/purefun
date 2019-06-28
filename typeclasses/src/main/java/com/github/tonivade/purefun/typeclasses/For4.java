/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple4;

public final class For4<F extends Kind, A, B, C, D> {

  private final Monad<F> monad;
  private final Higher1<F, A> value1;
  private final Higher1<F, B> value2;
  private final Higher1<F, C> value3;
  private final Higher1<F, D> value4;

  protected For4(Monad<F> monad,
                 Higher1<F, A> value1,
                 Higher1<F, B> value2,
                 Higher1<F, C> value3,
                 Higher1<F, D> value4) {
    this.monad = requireNonNull(monad);
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
    this.value3 = requireNonNull(value3);
    this.value4 = requireNonNull(value4);
  }

  public Higher1<F, Tuple4<A, B, C, D>> get() {
    return yield(Tuple4::of);
  }

  public <R> Higher1<F, R> yield(Function4<A, B, C, D, R> combine) {
    return monad.map4(value1, value2, value3, value4, combine);
  }

  public <R> R fix(Function1<Higher1<F, D>, R> mapper) {
    return mapper.apply(value4);
  }

  public void end(Consumer1<Higher1<F, D>> consumer) {
     consumer.accept(value4);
  }

  public <R> Higher1<F, R> returns(R value) {
    return monad.pure(value);
  }

  public <R> For5<F, A, B, C, D, R> map(Function1<D, R> mapper) {
    return For.with(monad, value1, value2, value3, value4, monad.map(value4, mapper));
  }

  public <R> For5<F, A, B, C, D, R> andThen(Producer<Higher1<F, R>> producer) {
    return For.with(monad, value1, value2, value3, value4, monad.flatMap(value4, i -> producer.get()));
  }

  public <R> For5<F, A, B, C, D, R> flatMap(Function1<D, ? extends Higher1<F, R>> mapper) {
    return For.with(monad, value1, value2, value3, value4, monad.flatMap(value4, mapper));
  }
}
