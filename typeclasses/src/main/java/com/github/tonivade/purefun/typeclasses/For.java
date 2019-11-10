/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.*;
import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;

public final class For<F extends Kind> {

  private final Monad<F> monad;

  private For(Monad<F> monad) {
    this.monad = requireNonNull(monad);
  }

  public <T> For1<F, T> and(Higher1<F, T> next) {
    return For.with(monad, next);
  }

  public <T> For1<F, T> andThen(Producer<Higher1<F, T>> next) {
    return For.with(monad, monad.andThen(monad.pure(unit()), next));
  }

  public static <F extends Kind> For<F> with(Monad<F> monad) {
    return new For<>(monad);
  }

  public static <F extends Kind, T> For1<F, T> with(Monad<F> monad,
                                                    Higher1<F, T> value1) {
    return new For1<>(monad, Producer.cons(value1));
  }

  public static <F extends Kind, T, R> For2<F, T, R> with(Monad<F> monad,
                                                          Higher1<F, T> value1,
                                                          Higher1<F, R> value2) {
    return new For2<>(monad, Producer.cons(value1), cons(value2));
  }

  public static <F extends Kind, A, B, C> For3<F, A, B, C> with(Monad<F> monad,
                                                                Higher1<F, A> value1,
                                                                Higher1<F, B> value2,
                                                                Higher1<F, C> value3) {
    return new For3<>(monad, Producer.cons(value1), cons(value2), cons(value3));
  }

  public static <F extends Kind, A, B, C, D> For4<F, A, B, C, D> with(Monad<F> monad,
                                                                      Higher1<F, A> value1,
                                                                      Higher1<F, B> value2,
                                                                      Higher1<F, C> value3,
                                                                      Higher1<F, D> value4) {
    return new For4<>(monad, Producer.cons(value1), cons(value2), cons(value3), cons(value4));
  }

  public static <F extends Kind, A, B, C, D, E> For5<F, A, B, C, D, E> with(Monad<F> monad,
                                                                            Higher1<F, A> value1,
                                                                            Higher1<F, B> value2,
                                                                            Higher1<F, C> value3,
                                                                            Higher1<F, D> value4,
                                                                            Higher1<F, E> value5) {
    return new For5<>(monad, Producer.cons(value1), cons(value2), cons(value3), cons(value4), cons(value5));
  }
}

abstract class AbstractFor<F extends Kind, A, B> {

  protected final Monad<F> monad;
  protected final Function1<A, ? extends Higher1<F, B>> value;

  protected AbstractFor(Monad<F> monad, Function1<A, ? extends Higher1<F, B>> value) {
    this.monad = requireNonNull(monad);
    this.value = requireNonNull(value);
  }

  public abstract Higher1<F, B> get();

  public <R> R fix(Function1<Higher1<F, B>, R> fix) {
    return fix.apply(get());
  }

  public void end(Consumer1<Higher1<F, B>> consumer) {
    consumer.accept(get());
  }

  public <R> Higher1<F, R> returns(R value) {
    return monad.pure(value);
  }
}
