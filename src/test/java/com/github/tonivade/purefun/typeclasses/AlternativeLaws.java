/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public class AlternativeLaws {

  private static final Function1<Integer, String> intToString = String::valueOf;
  private static final Function1<Integer, Integer> twoTimes = a -> a * 2;
  private static final Function1<Integer, Integer> plusFive = a -> a + 5;

  public static <F extends Kind> void verifyLaws(Alternative<F> instance) {
    assertAll(
        () -> rightAbsorption(instance),
        () -> leftDistributivity(instance),
        () -> rightDistributivity(instance));
  }

  private static <F extends Kind> void rightAbsorption(Alternative<F> instance) {
    assertEquals(
        instance.<String>zero(),
        instance.ap(instance.<Integer>zero(), instance.pure(intToString)),
        "rightAbsorption");
  }

  private static <F extends Kind> void leftDistributivity(Alternative<F> instance) {
    Higher1<F, Integer> fa = instance.pure(1);
    Higher1<F, Integer> fb = instance.pure(2);
    assertEquals(
        instance.map(instance.combineK(fa, fb), intToString),
        instance.combineK(instance.map(fa, intToString), instance.map(fb, intToString)),
        "leftDistributivity");
  }

  private static <F extends Kind> void rightDistributivity(Alternative<F> instance) {
    Higher1<F, Integer> fa = instance.pure(2);
    Higher1<F, Function1<Integer, Integer>> f1 = instance.pure(twoTimes);
    Higher1<F, Function1<Integer, Integer>> f2 = instance.pure(plusFive);
    assertEquals(
        instance.ap(fa, instance.combineK(f1, f2)),
        instance.combineK(instance.ap(fa, f1), instance.ap(fa, f2)),
        "rightDistributivity");
  }
}
