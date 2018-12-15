/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MappableLaws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;
  private final static Function1<String, String> toLowerCase = String::toLowerCase;

  public static <W extends Kind, F extends Mappable<W, String>> void verifyLaws(F functor) {
    assertAll(() -> identity(functor),
              () -> composition(functor),
              () -> associativity(functor));
  }

  private static <W extends Kind, F extends Mappable<W, String>> void identity(F functor) {
    assertEquals(functor, functor.map(Function1.identity()), "identity law");
  }

  private static <W extends Kind, F extends Mappable<W, String>> void composition(F functor) {
    assertEquals(functor.map(toUpperCase).map(toLowerCase),
                 functor.map(toUpperCase.andThen(toLowerCase)),
                 "composition law");
  }

  private static <W extends Kind, F extends Mappable<W, String>> void associativity(F functor) {
    assertEquals(functor.map(toUpperCase).map(toLowerCase.andThen(toUpperCase)),
                 functor.map(toUpperCase.andThen(toLowerCase)).map(toUpperCase),
                 "associativity law");
  }
}
