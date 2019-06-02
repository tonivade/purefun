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
import com.github.tonivade.purefun.typeclasses.Monad;

public class MonadLaws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;
  private final static Function1<String, String> toLowerCase = String::toLowerCase;

  public static <F extends Kind> void verifyLaws(Monad<F> monad) {
    assertAll(() -> leftIdentity(monad, monad.pure("hola mundo!")),
              () -> rightIdentity(monad, monad.pure("hola mundo!")),
              () -> associativity(monad, monad.pure("hola mundo!")));
  }

  private static <F extends Kind> void leftIdentity(Monad<F> monad, Higher1<F, String> value) {
    assertEquals(monad.map(value, toUpperCase),
                 monad.flatMap(value, string -> monad.pure(toUpperCase.apply(string))),
                 "left identity law");
  }

  private static <F extends Kind> void rightIdentity(Monad<F> monad, Higher1<F, String> value) {
    assertEquals(value,
                 monad.flatMap(value, string -> monad.pure(string)),
                 "right identity law");
  }

  private static <F extends Kind> void associativity(Monad<F> monad, Higher1<F, String> value) {
    assertEquals(monad.flatMap(monad.flatMap(value, v1 -> monad.pure(toUpperCase.apply(v1))), v2 -> monad.pure(toLowerCase.apply(v2))),
                 monad.flatMap(value, v1 -> monad.flatMap(monad.pure(toUpperCase.apply(v1)), v2 -> monad.pure(toLowerCase.apply(v2)))),
                 "associativity law");
  }
}
