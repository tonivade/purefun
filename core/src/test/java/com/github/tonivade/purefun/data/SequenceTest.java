/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.ImmutableList.toImmutableList;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.data.Sequence.zip;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;

public class SequenceTest {

  @Test
  public void zipTest() {
    ImmutableList<Tuple2<Integer, String>> zipped =
        zip(listOf(0, 1, 2), listOf("a", "b", "c")).collect(toImmutableList());

    assertEquals(listOf(Tuple.of(0, "a"), Tuple.of(1, "b"), Tuple.of(2, "c")), zipped);
  }

  @Test
  public void zipTestWithNulls() {
    ImmutableList<Tuple2<Integer, String>> zipped =
        zip(listOf(0, 1, 2), listOf("a", "b")).collect(toImmutableList());

    assertEquals(listOf(Tuple.of(0, "a"), Tuple.of(1, "b"), Tuple.of(2, null)), zipped);
  }

  @Test
  public void interleaveTest() {
    ImmutableList<String> interleaved =
        Sequence.interleave(listOf("0", "1", "2"), listOf("a", "b", "c")).collect(toImmutableList());

    assertEquals(listOf("0", "a", "1", "b", "2", "c"), interleaved);
  }

  @Test
  public void zipWithIndexTest() {
    ImmutableList<Tuple2<Integer, String>> zipped =
        listOf("a", "b", "c").zipWithIndex().collect(toImmutableList());

    assertEquals(listOf(Tuple.of(0, "a"), Tuple.of(1, "b"), Tuple.of(2, "c")), zipped);
  }
}
