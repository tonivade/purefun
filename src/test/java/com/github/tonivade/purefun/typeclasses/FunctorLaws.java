/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public class FunctorLaws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;
  private final static Function1<String, String> toLowerCase = String::toLowerCase;

  public static <F extends Kind> void verifyLaws(Functor<F> functor, Higher1<F, String> value) {
    assertAll(() -> identity(functor, value),
              () -> composition(functor, value),
              () -> associativity(functor, value));
  }

  private static <F extends Kind> void identity(Functor<F> functor, Higher1<F, String> value) {
    assertEquals(value, functor.map(value, Function1.identity()), "identity law");
  }

  private static <F extends Kind> void composition(Functor<F> functor, Higher1<F, String> value) {
    assertEquals(functor.map(functor.map(value, toUpperCase), toLowerCase),
                 functor.map(value, toUpperCase.andThen(toLowerCase)),
                 "composition law");
  }

  private static <F extends Kind> void associativity(Functor<F> functor, Higher1<F, String> value) {
    assertEquals(functor.map(functor.map(value, toUpperCase), toLowerCase.andThen(toUpperCase)),
                 functor.map(functor.map(value, toUpperCase.andThen(toLowerCase)), toUpperCase),
                 "associativity law");
  }
}
