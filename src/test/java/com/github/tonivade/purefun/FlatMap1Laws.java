/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlatMap1Laws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;
  private final static Function1<String, String> toLowerCase = String::toLowerCase;

  public static <W extends Kind, M extends FlatMap1<W, String>> void verifyLaws(M monad, Function1<String, M> unit) {
    assertAll(() -> leftIdentity(monad, unit),
              () -> rightIdentity(monad, unit),
              () -> associativity(monad, unit));
  }

  private static <W extends Kind, M extends FlatMap1<W, String>> void leftIdentity(M monad, Function1<String, M> unit) {
    assertEquals(monad.map(toUpperCase),
                 monad.flatMap(value -> unit.apply(toUpperCase.apply(value))),
                 "left identity law");
  }

  private static <W extends Kind, M extends FlatMap1<W, String>> void rightIdentity(M monad, Function1<String, M> unit) {
    assertEquals(monad,
                 monad.flatMap(value -> unit.apply(value)),
                 "right identity law");
  }

  private static <W extends Kind, M extends FlatMap1<W, String>> void associativity(M monad, Function1<String, M> unit) {
    assertEquals(monad.flatMap(v1 -> unit.apply(toUpperCase.apply(v1)))
                      .flatMap(v2 -> unit.apply(toLowerCase.apply(v2))),
                 monad.flatMap(v1 -> unit.apply(toUpperCase.apply(v1))
                      .flatMap(v2 -> unit.apply(toLowerCase.apply(v2)))),
                 "associativity law");
  }
}
