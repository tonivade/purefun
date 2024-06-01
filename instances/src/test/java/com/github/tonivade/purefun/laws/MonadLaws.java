/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Monad;

public class MonadLaws {

  public static <F extends Kind<F, ?>> void verifyLaws(Monad<F> monad) {
    assertAll(() -> leftIdentity(monad, monad.pure("hola mundo!"), String::toUpperCase),
              () -> rightIdentity(monad, monad.pure("hola mundo!")),
              () -> associativity(monad, monad.pure("hola mundo!"), String::toLowerCase, String::length));
  }

  private static <F extends Kind<F, ?>, A, B> void leftIdentity(Monad<F> monad, Kind<F, A> value, Function1<A, B> f) {
    assertEquals(monad.map(value, f),
                 monad.flatMap(value, a -> monad.pure(f.apply(a))),
                 "left identity law");
  }

  private static <F extends Kind<F, ?>, A> void rightIdentity(Monad<F> monad, Kind<F, A> value) {
    assertEquals(value,
                 monad.flatMap(value, monad::<A>pure),
                 "right identity law");
  }

  private static <F extends Kind<F, ?>, A, B, C> void associativity(Monad<F> monad,
                                                              Kind<F, A> value,
                                                              Function1<A, B> f,
                                                              Function1<B, C> g) {
    assertEquals(monad.flatMap(monad.flatMap(value, v1 -> monad.pure(f.apply(v1))), v2 -> monad.pure(g.apply(v2))),
                 monad.flatMap(value, v1 -> monad.flatMap(monad.pure(f.apply(v1)), v2 -> monad.pure(g.apply(v2)))),
                 "associativity law");
  }
}
