/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PartialFunction2Test {

  @Test
  public void div() {
    PartialFunction2<Integer, Integer, Double> div =
        PartialFunction2.of((a, b) -> b > 0, (a, b) -> ((double) a / (double) b));

    assertAll(
        () -> assertFalse(div.isDefinedAt(1, 0)),
        () -> assertTrue(div.isDefinedAt(0, 1)),
        () -> assertEquals(Double.valueOf(0.), div.apply(0, 1)));
  }
}
