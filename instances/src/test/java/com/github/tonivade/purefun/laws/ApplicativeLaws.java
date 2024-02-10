/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static com.github.tonivade.purefun.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;
import com.github.tonivade.purefun.typeclasses.Applicative;

public class ApplicativeLaws {

  public static <F extends Witness> void verifyLaws(Applicative<F> applicative) {
    assertAll(() -> identityLaw(applicative, applicative.pure("hola mundo!")),
              () -> homomorphismLaw(applicative, "hola mundo!", String::toUpperCase),
              () -> interchangeLaw(applicative, "hola mundo!", String::toUpperCase),
              () -> mapLaw(applicative, applicative.pure("hola mundo!"), String::toUpperCase));
  }

  private static <F extends Witness, A> void identityLaw(Applicative<F> applicative, Kind<F, A> value) {
    assertEquals(value, applicative.ap(value, applicative.pure(identity())), "identity law");
  }

  private static <F extends Witness, A, B> void homomorphismLaw(Applicative<F> applicative, A value, Function1<A, B> f) {
    assertEquals(
        applicative.pure(f.apply(value)),
        applicative.ap(applicative.pure(value), applicative.pure(f)),
        "homomorphism law");
  }

  private static <F extends Witness, A, B> void interchangeLaw(Applicative<F> applicative, A value, Function1<A, B> f) {
    assertEquals(
        applicative.ap(applicative.pure(value), applicative.pure(f)),
        applicative.ap(applicative.pure(f), applicative.pure(a -> f.apply(value))),
        "interchange law");
  }

  private static <F extends Witness, A, B> void mapLaw(Applicative<F> applicative, Kind<F, A> value, Function1<A, B> f) {
    assertEquals(
        applicative.map(value, f),
        applicative.ap(value, applicative.pure(f)), "map law");
  }
}
