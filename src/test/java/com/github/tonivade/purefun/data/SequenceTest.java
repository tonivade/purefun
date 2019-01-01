/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.ImmutableList.toImmutableList;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.data.Sequence.zip;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.typeclasses.Eq;

public class SequenceTest {

  @Test
  public void zipTest() {
    ImmutableList<Tuple2<Integer, String>> zipped =
        zip(listOf(0, 1, 2), listOf("a", "b", "c")).collect(toImmutableList());

    assertEquals(listOf(Tuple.of(0, "a"), Tuple.of(1, "b"), Tuple.of(2, "c")), zipped);
  }

  @Test
  public void eq() {
    Eq<Higher1<Sequence.µ, Integer>> instance = Sequence.eq(Eq.object());

    assertAll(
        () -> assertTrue(instance.eqv(listOf(1, 2, 3), listOf(1, 2, 3))),
        () -> assertFalse(instance.eqv(listOf(1, 2, 3), listOf(3, 2, 1))),
        () -> assertFalse(instance.eqv(listOf(1, 2), listOf(1, 2, 3))));
  }
}
