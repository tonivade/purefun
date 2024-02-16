/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Functor;

public class FunctorLaws {

  public static <F extends Witness> void verifyLaws(Functor<F> functor, Kind<F, String> value) {
    assertAll(() -> identity(functor, value),
              () -> composition(functor, value, String::toUpperCase, String::length));
  }

  private static <F extends Witness, A> void identity(Functor<F> functor, Kind<F, A> value) {
    assertEquals(value, functor.map(value, Function1.identity()), "identity law");
  }

  private static <F extends Witness, A, B, C> void composition(Functor<F> functor,
                                                            Kind<F, A> value,
                                                            Function1<A, B> f,
                                                            Function1<B, C> g) {
    assertEquals(functor.map(functor.map(value, f), g),
                 functor.map(value, f.andThen(g)),
                 "composition law");
  }
}
