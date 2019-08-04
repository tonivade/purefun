/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static com.github.tonivade.purefun.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.typeclasses.Contravariant;

public class ContravariatLaws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;
  private final static Function1<String, String> toLowerCase = String::toLowerCase;

  public static <F extends Kind> void verifyLaws(Contravariant<F> instance, Higher1<F, String> value) {
    identityLaw(instance, value);
    compositionLaw(instance, value);
  }

  private static <F extends Kind> void identityLaw(Contravariant<F> instance, Higher1<F, String> value) {
    assertEquals(value, instance.contramap(value, identity()), "identity law");
  }

  private static <F extends Kind> void compositionLaw(Contravariant<F> instance, Higher1<F, String> value) {
    assertEquals(instance.contramap(instance.contramap(value, toUpperCase), toLowerCase),
                 instance.contramap(value, toLowerCase.compose(toUpperCase)),
                 "composition law");
  }
}
