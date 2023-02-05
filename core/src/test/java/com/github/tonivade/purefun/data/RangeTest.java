/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.data.Sequence.arrayOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RangeTest {

  @Test
  public void range() {
    Range range = Range.of(1, 10);

    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> Range.of(1, 0)),
        () -> assertEquals(0, Range.of(1, 1).size()),
        () -> assertEquals(1, range.begin()),
        () -> assertEquals(10, range.end()),
        () -> assertEquals(9, range.size()),
        () -> assertFalse(range.contains(0)),
        () -> assertFalse(range.contains(10)),
        () -> assertTrue(range.contains(1)),
        () -> assertEquals(arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9), range.collect())
    );
  }
}