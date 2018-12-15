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

public class MonadLaws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;
  private final static Function1<String, String> toLowerCase = String::toLowerCase;

  public static <W extends Kind, M extends Monad<W>> void verifyLaws(M monad, Higher1<W, String> value) {
    assertAll(() -> leftIdentity(monad, value),
              () -> rightIdentity(monad, value),
              () -> associativity(monad, value));
  }

  private static <W extends Kind, M extends Monad<W>> void leftIdentity(M monad, Higher1<W, String> value) {
    assertEquals(monad.map(value, toUpperCase),
                 monad.flatMap(value, string -> monad.pure(toUpperCase.apply(string))),
                 "left identity law");
  }

  private static <W extends Kind, M extends Monad<W>> void rightIdentity(M monad, Higher1<W, String> value) {
    assertEquals(value,
                 monad.flatMap(value, string -> monad.pure(string)),
                 "right identity law");
  }

  private static <W extends Kind, M extends Monad<W>> void associativity(M monad, Higher1<W, String> value) {
    assertEquals(monad.flatMap(monad.flatMap(value, v1 -> monad.pure(toUpperCase.apply(v1))), v2 -> monad.pure(toLowerCase.apply(v2))),
                 monad.flatMap(value, v1 -> monad.flatMap(monad.pure(toUpperCase.apply(v1)), v2 -> monad.pure(toLowerCase.apply(v2)))),
                 "associativity law");
  }
}
