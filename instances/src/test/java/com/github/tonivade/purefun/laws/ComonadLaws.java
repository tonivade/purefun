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
import com.github.tonivade.purefun.typeclasses.Comonad;

public class ComonadLaws {

  public static <F extends Kind> void verifyLaws(Comonad<F> comonad, Higher1<F, String> value) {
    assertAll(
        () -> extractCoflattenIdentity(comonad, value),
        () -> mapCoflattenIdentity(comonad, value),
        () -> mapCoflatMapCoherence(comonad, value, String::toUpperCase),
        () -> comonadLeftIdentity(comonad, value),
        () -> comonadRightIdentity(comonad, value, comonad::extract));
  }

  private static <F extends Kind, A> void extractCoflattenIdentity(Comonad<F> comonad, Higher1<F, A> value) {
    assertEquals(value, comonad.extract(comonad.coflatten(value)), "extract coflatten identity");
  }

  private static <F extends Kind, A> void mapCoflattenIdentity(Comonad<F> comonad, Higher1<F, A> value) {
    assertEquals(value, comonad.map(comonad.coflatten(value), comonad::extract), "map coflatten identity");
  }

  private static <F extends Kind, A, B> void mapCoflatMapCoherence(Comonad<F> comonad,
                                                                   Higher1<F, A> value,
                                                                   Function1<A, B> f) {
    assertEquals(
        comonad.map(value, f),
        comonad.coflatMap(value, f.compose(comonad::extract)),
        "map coflatMap coherence");
  }

  private static <F extends Kind, A> void comonadLeftIdentity(Comonad<F> comonad, Higher1<F, A> value) {
    assertEquals(value, comonad.coflatMap(value, comonad::extract), "comonad left identity");
  }

  private static <F extends Kind, A, B> void comonadRightIdentity(Comonad<F> comonad,
                                                                  Higher1<F, A> value,
                                                                  Function1<Higher1<F, A>, B> f) {
    assertEquals(
        comonad.extract(comonad.coflatMap(value, f)),
        f.apply(value),
        "comonad right identity");
  }
}
