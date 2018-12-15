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

  public static <W extends Kind, F extends Functor<W>> void verifyLaws(F functor, Higher1<W, String> value) {
    assertAll(() -> identity(functor, value),
              () -> composition(functor, value),
              () -> associativity(functor, value));
  }

  private static <W extends Kind, F extends Functor<W>> void identity(F functor, Higher1<W, String> value) {
    assertEquals(value, functor.map(value, Function1.identity()), "identity law");
  }

  private static <W extends Kind, F extends Functor<W>> void composition(F functor, Higher1<W, String> value) {
    assertEquals(functor.map(functor.map(value, toUpperCase), toLowerCase),
                 functor.map(value, toUpperCase.andThen(toLowerCase)),
                 "composition law");
  }

  private static <W extends Kind, F extends Functor<W>> void associativity(F functor, Higher1<W, String> value) {
    assertEquals(functor.map(functor.map(value, toUpperCase), toLowerCase.andThen(toUpperCase)),
                 functor.map(functor.map(value, toUpperCase.andThen(toLowerCase)), toUpperCase),
                 "associativity law");
  }
}
