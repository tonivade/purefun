/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple1;

public final class For1<F extends Kind, A> extends AbstractFor<F, A> {

  protected For1(Monad<F> monad, Higher1<F, A> value) {
    super(monad, value);
  }

  public Higher1<F, Tuple1<A>> tuple() {
    return monad.map(value, Tuple::of);
  }

  public <R> Higher1<F, R> yield(Function1<A, R> combinator) {
    return monad.map(value, combinator);
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
