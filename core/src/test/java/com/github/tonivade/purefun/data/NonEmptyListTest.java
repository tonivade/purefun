/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NonEmptyListTest {

  @Test
  public void test() {
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> NonEmptyList.of(null)),
        () -> assertThrows(IllegalArgumentException.class, () -> NonEmptyList.of(ImmutableList.empty())),
        () -> assertDoesNotThrow(() -> NonEmptyList.of(1))
    );
  }
}