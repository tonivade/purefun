/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static com.github.tonivade.purefun.core.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Contravariant;

public class ContravariatLaws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;
  private final static Function1<String, String> toLowerCase = String::toLowerCase;

  public static <F extends Kind<F, ?>> void verifyLaws(Contravariant<F> instance, Kind<F, String> value) {
    assertAll(
      () -> identityLaw(instance, value),
      () -> compositionLaw(instance, value, toUpperCase, toLowerCase)
    );
  }

  private static <F extends Kind<F, ?>, A> void identityLaw(Contravariant<F> instance, Kind<F, A> value) {
    assertEquals(value, instance.contramap(value, identity()), "identity law");
  }

  private static <F extends Kind<F, ?>, A, B, C> void compositionLaw(Contravariant<F> instance,
                                                               Kind<F, A> value,
                                                               Function1<B, A> f,
                                                               Function1<C, B> g) {
    assertEquals(instance.contramap(instance.contramap(value, f), g),
                 instance.contramap(value, f.compose(g)),
                 "composition law");
  }
}
