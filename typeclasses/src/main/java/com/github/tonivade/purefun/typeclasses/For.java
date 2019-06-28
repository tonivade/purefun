/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;

public final class For<F extends Kind> {

  private final Monad<F> monad;

  private For(Monad<F> monad) {
    this.monad = requireNonNull(monad);
  }

  public <T> For1<F, T> andThen(Producer<Higher1<F, T>> value) {
    return For.with(monad, value.get());
  }

  public static <F extends Kind> For<F> with(Monad<F> monad) {
    return new For<>(monad);
  }

  public static <F extends Kind, T> For1<F, T> with(Monad<F> monad,
                                                    Higher1<F, T> value) {
    return new For1<>(monad, value);
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
