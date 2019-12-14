/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.typeclasses.Functor;

public class FunctorLaws {

  public static <F extends Kind> void verifyLaws(Functor<F> functor, Higher1<F, String> value) {
    assertAll(() -> identity(functor, value),
              () -> composition(functor, value, String::toUpperCase, String::length));
  }

  private static <F extends Kind, A> void identity(Functor<F> functor, Higher1<F, A> value) {
    assertEquals(value, functor.map(value, Function1.identity()), "identity law");
  }

  private static <F extends Kind, A, B, C> void composition(Functor<F> functor,
                                                            Higher1<F, A> value,
                                                            Function1<A, B> f,
                                                            Function1<B, C> g) {
    assertEquals(functor.map(functor.map(value, f), g),
                 functor.map(value, f.andThen(g)),
                 "composition law");
  }
}
