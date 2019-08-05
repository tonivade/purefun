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

public final class For<F extends Kind> {

  private final Monad<F> monad;

  private For(Monad<F> monad) {
    this.monad = requireNonNull(monad);
  }

  public <T> For1<F, T> and(Higher1<F, T> next) {
    return For.with(monad, next);
  }

  public <T> For1<F, T> andThen(Producer<Higher1<F, T>> next) {
    return and(next.get());
  }

  public static <F extends Kind> For<F> with(Monad<F> monad) {
    return new For<>(monad);
  }

  public static <F extends Kind, T> For1<F, T> with(Monad<F> monad,
                                                    Higher1<F, T> value1) {
    return new For1<>(monad, value1);
  }

  public static <F extends Kind, T, R> For2<F, T, R> with(Monad<F> monad,
                                                          Higher1<F, T> value1,
                                                          Higher1<F, R> value2) {
    return new For2<>(monad, value1, value2);
  }

  public static <F extends Kind, A, B, C> For3<F, A, B, C> with(Monad<F> monad,
                                                                Higher1<F, A> value1,
                                                                Higher1<F, B> value2,
                                                                Higher1<F, C> value3) {
    return new For3<>(monad, value1, value2, value3);
  }

  public static <F extends Kind, A, B, C, D> For4<F, A, B, C, D> with(Monad<F> monad,
                                                                      Higher1<F, A> value1,
                                                                      Higher1<F, B> value2,
                                                                      Higher1<F, C> value3,
                                                                      Higher1<F, D> value4) {
    return new For4<>(monad, value1, value2, value3, value4);
  }

  public static <F extends Kind, A, B, C, D, E> For5<F, A, B, C, D, E> with(Monad<F> monad,
                                                                            Higher1<F, A> value1,
                                                                            Higher1<F, B> value2,
                                                                            Higher1<F, C> value3,
                                                                            Higher1<F, D> value4,
                                                                            Higher1<F, E> value5) {
    return new For5<>(monad, value1, value2, value3, value4, value5);
  }
}

abstract class AbstractFor<F extends Kind, A> {

  protected final Monad<F> monad;
  protected final Higher1<F, A> value;

  protected AbstractFor(Monad<F> monad, Higher1<F, A> value) {
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
}
