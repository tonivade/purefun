/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.Precondition.checkNegative;
import static com.github.tonivade.purefun.Precondition.checkNonEmpty;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Precondition.checkPositive;
import static com.github.tonivade.purefun.Precondition.checkRange;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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