/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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

  public static <F extends Kind> void verifyLaws(Applicative<F> applicative) {
    assertAll(() -> identityLaw(applicative, applicative.pure("hola mundo!")),
              () -> homomorphismLaw(applicative, "hola mundo!"),
              () -> interchangeLaw(applicative, "hola mundo!"),
              () -> mapLaw(applicative, applicative.pure("hola mundo!")));
  }

  private static <F extends Kind> void identityLaw(Applicative<F> applicative, Higher1<F, String> value) {
    assertEquals(value, applicative.ap(value, applicative.pure(identity())), "identity law");
  }

  private static <F extends Kind> void homomorphismLaw(Applicative<F> applicative, String value) {
    assertEquals(applicative.pure(toUpperCase.apply(value)),
        applicative.ap(applicative.pure(value), applicative.pure(toUpperCase)),
        "homomorphism law");
  }

  private static <F extends Kind> void interchangeLaw(Applicative<F> applicative, String value) {
    assertEquals(applicative.ap(applicative.pure(value), applicative.pure(toUpperCase)),
        applicative.ap(applicative.pure(toUpperCase), applicative.pure(a -> toUpperCase.apply(value))),
        "interchange law");
  }

  private static <F extends Kind> void mapLaw(Applicative<F> applicative, Higher1<F, String> value) {
    assertEquals(applicative.map(value, toUpperCase), applicative.ap(value, applicative.pure(toUpperCase)), "map law");
  }
}
