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

  private static final Function1<String, String> toUpperCase = String::toUpperCase;

  public static <F extends Kind> void verifyLaws(Comonad<F> comonad, Higher1<F, String> value) {
    assertAll(
        () -> extractCoflattenIdentity(comonad, value),
        () -> mapCoflattenIdentity(comonad, value),
        () -> mapCoflatMapCoherence(comonad, value),
        () -> comonadLeftIdentity(comonad, value),
        () -> comonadRightIdentity(comonad, value, comonad::extract));
  }

  private static <F extends Kind> void extractCoflattenIdentity(Comonad<F> comonad, Higher1<F, String> value) {
    assertEquals(value, comonad.extract(comonad.coflatten(value)), "extract coflatten identity");
  }

  private static <F extends Kind> void mapCoflattenIdentity(Comonad<F> comonad, Higher1<F, String> value) {
    assertEquals(value, comonad.map(comonad.coflatten(value), comonad::extract), "map coflatten identity");
  }

  private static <F extends Kind> void mapCoflatMapCoherence(Comonad<F> comonad, Higher1<F, String> value) {
    assertEquals(
        comonad.map(value, toUpperCase),
        comonad.coflatMap(value, toUpperCase.compose(comonad::extract)),
        "map coflatMap coherence");
  }

  private static <F extends Kind> void comonadLeftIdentity(Comonad<F> comonad, Higher1<F, String> value) {
    assertEquals(value, comonad.coflatMap(value, comonad::extract), "comonad left identity");
  }

  private static <F extends Kind> void comonadRightIdentity(Comonad<F> comonad, Higher1<F, String> value,
      Function1<Higher1<F, String>, String> coflatMap) {
    assertEquals(
        coflatMap.andThen(toUpperCase).apply(value),
        comonad.extract(comonad.coflatMap(value, coflatMap.andThen(toUpperCase))),
        "comonad right identity");
  }
}
