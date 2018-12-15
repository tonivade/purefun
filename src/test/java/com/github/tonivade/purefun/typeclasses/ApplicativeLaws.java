/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public class ApplicativeLaws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;

  public static <W extends Kind> void verifyLaws(Applicative<W> applicative) {
    assertAll(() -> identityLaw(applicative, applicative.pure("hola mundo!")),
              () -> homomorphismLaw(applicative, "hola mundo!"),
              () -> interchangeLaw(applicative, "hola mundo!"),
              () -> mapLaw(applicative, applicative.pure("hola mundo!")));
  }

  private static <W extends Kind> void identityLaw(Applicative<W> applicative, Higher1<W, String> value) {
    assertEquals(value, applicative.ap(value, applicative.pure(identity())), "identity law");
  }

  private static <W extends Kind> void homomorphismLaw(Applicative<W> applicative, String value) {
    assertEquals(applicative.pure(toUpperCase.apply(value)),
        applicative.ap(applicative.pure(value), applicative.pure(toUpperCase)),
        "homomorphism law");
  }

  private static <W extends Kind> void interchangeLaw(Applicative<W> applicative, String value) {
    assertEquals(applicative.ap(applicative.pure(value), applicative.pure(toUpperCase)),
        applicative.ap(applicative.pure(toUpperCase), applicative.pure(a -> toUpperCase.apply(value))),
        "interchange law");
  }

  private static <W extends Kind> void mapLaw(Applicative<W> applicative, Higher1<W, String> value) {
    assertEquals(applicative.map(value, toUpperCase), applicative.ap(value, applicative.pure(toUpperCase)), "map law");
  }
}
