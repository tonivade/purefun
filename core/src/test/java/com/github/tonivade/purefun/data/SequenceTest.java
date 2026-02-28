/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.ImmutableList.toImmutableList;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.data.Sequence.zip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;

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

    assertEquals(listOf(Tuple.of(0, "a"), Tuple.of(1, "b")), zipped);
  }

  @Test
  public void interleaveTest() {
    ImmutableList<String> interleaved =
        Sequence.interleave(listOf("0", "1", "2"), listOf("a", "b", "c")).collect(toImmutableList());

    assertEquals(listOf("0", "a", "1", "b", "2", "c"), interleaved);
  }

  @Test
  void toCollection() {
    var list = Sequence.listOf(1, 2, 3, 4);

    assertThat(list.toCollection()).containsExactly(1, 2, 3, 4);
    assertThat(list.toSequencedCollection()).containsExactly(1, 2, 3, 4);
    assertThat(list.toSequencedCollection().reversed()).containsExactly(4, 3, 2, 1);
  }

  @Test
  void toArray() {
    var list = Sequence.listOf(1, 2, 3, 4);

    assertThat(list.toCollection().toArray()).containsExactly(1, 2, 3, 4);
    assertThat(list.toCollection().toArray(new Object[] {})).containsExactly(1, 2, 3, 4);
    assertThat(list.toCollection().toArray(new Object[4])).containsExactly(1, 2, 3, 4);
    assertThat(list.toCollection().toArray(new Object[5])).containsExactly(1, 2, 3, 4, null);
    assertThat(list.toCollection().toArray(Object[]::new)).containsExactly(1, 2, 3, 4);
    assertThat(list.toSequencedCollection().toArray()).containsExactly(1, 2, 3, 4);
    assertThat(list.toSequencedCollection().toArray(new Object[] {})).containsExactly(1, 2, 3, 4);
    assertThat(list.toSequencedCollection().toArray(new Object[4])).containsExactly(1, 2, 3, 4);
    assertThat(list.toSequencedCollection().toArray(new Object[5])).containsExactly(1, 2, 3, 4, null);
    assertThat(list.toSequencedCollection().toArray(Object[]::new)).containsExactly(1, 2, 3, 4);
    assertThat(list.toSequencedCollection().reversed().toArray()).containsExactly(4, 3, 2, 1);
    assertThat(list.toSequencedCollection().reversed().toArray(new Object[] {})).containsExactly(4, 3, 2, 1);
    assertThat(list.toSequencedCollection().reversed().toArray(new Object[4])).containsExactly(4, 3, 2, 1);
    assertThat(list.toSequencedCollection().reversed().toArray(new Object[5])).containsExactly(4, 3, 2, 1, null);
    assertThat(list.toSequencedCollection().reversed().toArray(Object[]::new)).containsExactly(4, 3, 2, 1);
  }
}
