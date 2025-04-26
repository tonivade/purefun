/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Precondition.checkNegative;
import static com.github.tonivade.purefun.core.Precondition.checkNonEmpty;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Precondition.checkPositive;
import static com.github.tonivade.purefun.core.Precondition.checkRange;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PreconditionTest {

  @Test
  public void notValid() {
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> checkNonNull(null)),
        () -> assertThrows(IllegalArgumentException.class, () -> checkNonEmpty(null)),
        () -> assertThrows(IllegalArgumentException.class, () -> checkNonEmpty("")),
        () -> assertThrows(IllegalArgumentException.class, () -> checkPositive(0)),
        () -> assertThrows(IllegalArgumentException.class, () -> checkNegative(0)),
        () -> assertThrows(IllegalArgumentException.class, () -> checkRange(1, 1, 1)),
        () -> assertThrows(IllegalArgumentException.class, () -> checkRange(-1, 0, 1)),
        () -> assertThrows(IllegalArgumentException.class, () -> checkRange(1, 0, 1))
    );
  }

  @Test
  public void valid() {
    assertAll(
        () -> assertEquals("", checkNonNull("")),
        () -> assertEquals("s", checkNonEmpty("s")),
        () -> assertEquals(1, checkPositive(1)),
        () -> assertEquals(-1, checkNegative(-1)),
        () -> assertEquals(0, checkRange(0, 0, 1))
    );
  }
}