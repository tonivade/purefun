/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;

public final class For<F extends Kind> {

  private final Monad<F> monad;

  private For(Monad<F> monad) {
    this.monad = checkNonNull(monad);
  }

  public <T> For1<F, T> and(T next) {
    return For.with(monad, monad.pure(next));
  }

  public <T> For1<F, T> then(Higher1<F, T> next) {
    return For.with(monad, next);
  }

  public <T> For1<F, T> andThen(Producer<? extends Higher1<F, T>> next) {
    return For.with(monad, monad.andThen(monad.pure(unit()), next));
  }

  public static <F extends Kind> For<F> with(Monad<F> monad) {
    return new For<>(monad);
  }

  public static <F extends Kind, T> For1<F, T> with(Monad<F> monad, Higher1<F, T> value1) {
    return new For1<>(monad, cons(value1));
  }
}

abstract class AbstractFor<F extends Kind, A, B> {

  protected final Monad<F> monad;
  protected final Function1<A, ? extends Higher1<F, B>> value;

  protected AbstractFor(Monad<F> monad, Function1<A, ? extends Higher1<F, B>> value) {
    this.monad = checkNonNull(monad);
    this.value = checkNonNull(value);
  }

  public abstract Higher1<F, B> run();

  public <R> R fix(Function1<Higher1<F, B>, R> fix) {
    return fix.apply(run());
  }

  public void end(Consumer1<Higher1<F, B>> consumer) {
    consumer.accept(run());
  }

  public <R> Higher1<F, R> returns(R value) {
    return monad.map(run(), ignore -> value);
  }
}
