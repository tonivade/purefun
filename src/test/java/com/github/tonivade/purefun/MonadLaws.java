/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonadLaws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;
  private final static Function1<String, String> toLowerCase = String::toLowerCase;

  public static <W extends Witness> void verifyLaws(Monad<W, String> monad,
                                                    Function1<String, Monad<W, String>> unit) {
    assertAll(() -> leftIdentity(monad, unit),
              () -> rightIdentity(monad, unit),
              () -> associativity(monad, unit));
  }

  private static <W extends Witness> void leftIdentity(Monad<W, String> monad,
                                                       Function1<String, Monad<W, String>> unit) {
    assertEquals(monad.map(toUpperCase),
                 monad.flatMap(value -> unit.apply(toUpperCase.apply(value))),
                 "left identity law");
  }

  private static <W extends Witness> void rightIdentity(Monad<W, String> monad,
                                                        Function1<String, Monad<W, String>> unit) {
    assertEquals(monad,
                 monad.flatMap(value -> unit.apply(value)),
                 "right identity law");
  }

  private static <W extends Witness> void associativity(Monad<W, String> monad,
                                                        Function1<String, Monad<W, String>> unit) {
    assertEquals(monad.flatMap(v1 -> unit.apply(toUpperCase.apply(v1)))
                      .flatMap(v2 -> unit.apply(toLowerCase.apply(v2))),
                 monad.flatMap(v1 -> unit.apply(toUpperCase.apply(v1))
                      .flatMap(v2 -> unit.apply(toLowerCase.apply(v2)))),
                 "associativity law");
  }
}
