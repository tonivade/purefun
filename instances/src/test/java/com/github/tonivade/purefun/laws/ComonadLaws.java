/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Comonad;

public class ComonadLaws {

  public static <F extends Kind<F, ?>> void verifyLaws(Comonad<F> comonad, Kind<F, String> value) {
    assertAll(
        () -> extractCoflattenIdentity(comonad, value),
        () -> mapCoflattenIdentity(comonad, value),
        () -> mapCoflatMapCoherence(comonad, value, String::toUpperCase),
        () -> comonadLeftIdentity(comonad, value),
        () -> comonadRightIdentity(comonad, value, comonad::extract));
  }

  private static <F extends Kind<F, ?>, A> void extractCoflattenIdentity(Comonad<F> comonad, Kind<F, A> value) {
    assertEquals(value, comonad.extract(comonad.coflatten(value)), "extract coflatten identity");
  }

  private static <F extends Kind<F, ?>, A> void mapCoflattenIdentity(Comonad<F> comonad, Kind<F, A> value) {
    assertEquals(value, comonad.map(comonad.coflatten(value), comonad::extract), "map coflatten identity");
  }

  private static <F extends Kind<F, ?>, A, B> void mapCoflatMapCoherence(Comonad<F> comonad,
                                                                      Kind<F, A> value,
                                                                      Function1<? super A, ? extends B> f) {
    assertEquals(
        comonad.map(value, f),
        comonad.coflatMap(value, f.compose(comonad::extract)),
        "map coflatMap coherence");
  }

  private static <F extends Kind<F, ?>, A> void comonadLeftIdentity(Comonad<F> comonad, Kind<F, A> value) {
    assertEquals(value, comonad.coflatMap(value, comonad::extract), "comonad left identity");
  }

  private static <F extends Kind<F, ?>, A, B> void comonadRightIdentity(Comonad<F> comonad,
                                                                  Kind<F, A> value,
                                                                  Function1<? super Kind<F, ? extends A>, ? extends B> f) {
    assertEquals(
        comonad.extract(comonad.coflatMap(value, f)),
        f.apply(value),
        "comonad right identity");
  }
}
