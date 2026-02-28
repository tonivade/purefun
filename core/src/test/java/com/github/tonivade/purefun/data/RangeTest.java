/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.data.Sequence.arrayOf;

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

  @Test
  public void revRange() {
    Range range = Range.of(1, 10).reverse();

    assertAll(
        () -> assertEquals(0, Range.of(1, 1).reverse().size()),
        () -> assertEquals(10, range.begin()),
        () -> assertEquals(1, range.end()),
        () -> assertEquals(9, range.size()),
        () -> assertFalse(range.contains(0)),
        () -> assertFalse(range.contains(10)),
        () -> assertTrue(range.contains(1)),
        () -> assertEquals(arrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1), range.collect())
    );
  }

  @Test
  public void validRange() {
    assertThrows(IllegalArgumentException.class, () -> Range.of(10, 1));
  }
}