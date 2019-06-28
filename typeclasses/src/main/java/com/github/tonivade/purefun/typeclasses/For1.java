/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;

public final class For1<F extends Kind, A> {

  private final Monad<F> monad;
  private final Higher1<F, A> value;

  protected For1(Monad<F> monad, Higher1<F, A> value) {
    this.monad = requireNonNull(monad);
    this.value = requireNonNull(value);
  }

  public Higher1<F, A> get() {
    return value;
  }

  public <R> R fix(Function1<Higher1<F, A>, R> mapper) {
    return mapper.apply(value);
  }

  public void end(Consumer1<Higher1<F, A>> consumer) {
     consumer.accept(value);
  }

  public <R> Higher1<F, R> returns(R value) {
    return monad.pure(value);
  }

  public <R> For2<F, A, R> map(Function1<A, R> mapper) {
    return For.with(monad, value, monad.map(value, mapper));
  }

  public <R> For2<F, A, R> andThen(Producer<Higher1<F, R>> producer) {
    return For.with(monad, value, monad.flatMap(value, i -> producer.get()));
  }

  public <R> For2<F, A, R> flatMap(Function1<A, ? extends Higher1<F, R>> mapper) {
    return For.with(monad, value, monad.flatMap(value, mapper));
  }
}
