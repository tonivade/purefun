/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FunctorLaws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;
  private final static Function1<String, String> toLowerCase = String::toLowerCase;

  public static <W extends Witness> void verifyLaws(Functor<W, String> functor) {
    assertAll(() -> identity(functor),
              () -> composition(functor),
              () -> associativity(functor));
  }

  private static <W extends Witness> void identity(Functor<W, String> functor) {
    assertEquals(functor, functor.map(Function1.identity()), "identity law");
  }

  private static <W extends Witness> void composition(Functor<W, String> functor) {
    assertEquals(functor.map(toUpperCase).map(toLowerCase),
                 functor.map(toUpperCase.andThen(toLowerCase)),
                 "composition law");
  }

  private static <W extends Witness> void associativity(Functor<W, String> functor) {
    assertEquals(functor.map(toUpperCase).map(toLowerCase.andThen(toUpperCase)),
                 functor.map(toUpperCase.andThen(toLowerCase)).map(toUpperCase),
                 "associativity law");
  }
}
