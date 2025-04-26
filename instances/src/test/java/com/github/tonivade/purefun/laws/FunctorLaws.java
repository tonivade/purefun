/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Functor;

public class FunctorLaws {

  public static <F extends Kind<F, ?>> void verifyLaws(Functor<F> functor, Kind<F, String> value) {
    assertAll(() -> identity(functor, value),
              () -> composition(functor, value, String::toUpperCase, String::length));
  }

  private static <F extends Kind<F, ?>, A> void identity(Functor<F> functor, Kind<F, A> value) {
    assertEquals(value, functor.map(value, Function1.identity()), "identity law");
  }

  private static <F extends Kind<F, ?>, A, B, C> void composition(Functor<F> functor,
                                                            Kind<F, A> value,
                                                            Function1<A, B> f,
                                                            Function1<B, C> g) {
    assertEquals(functor.map(functor.map(value, f), g),
                 functor.map(value, f.andThen(g)),
                 "composition law");
  }
}
