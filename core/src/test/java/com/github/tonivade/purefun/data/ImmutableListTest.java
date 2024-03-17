/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.data.ImmutableList.toImmutableList;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.type.Option;

public class ImmutableListTest {

  private final Function1<String, String> toUpperCase = String::toUpperCase;

  @Test
  public void nonEmptyList() {
    ImmutableList<String> list = listOf("a", "b", "c");

    assertAll(
      () -> assertEquals(3, list.size()),
      () -> assertEquals(Option.some("a"), list.head()),
      () -> assertEquals(listOf("b", "c"), list.tail()),
      () -> assertFalse(list.isEmpty()),
      () -> assertArrayEquals(new String[]{"a", "b", "c"}, list.toArray(String[]::new)),
      () -> assertTrue(list.contains("a")),
      () -> assertTrue(list.containsAll(listOf("a", "b"))),
      () -> assertFalse(list.contains("z")),
      () -> assertFalse(list.contains(new Object())),
      () -> assertFalse(list.containsAll(listOf("a", "z"))),
      () -> assertEquals("abc", list.fold("", (a, b) -> a + b)),
      () -> assertEquals("abc", list.foldLeft("", (a, b) -> a + b)),
      () -> assertEquals("abc", list.foldRight("", (a, b) -> a + b)),
      () -> assertEquals(Option.some("abc"), list.reduce((a, b) -> a + b)),
      () -> assertEquals(listOf("a", "b", "c"), list),
      () -> assertEquals(listOf("c", "b", "a"), list.reverse()),
      () -> assertEquals(listOf("c", "b", "a"), list.sort((a, b) -> b.compareTo(a))),
      () -> assertEquals(ImmutableList.from(asList("a", "b", "c")), list),
      () -> assertEquals(asList("a", "b", "c"), list.toList()),
      () -> assertEquals(listOf("c"), list.drop(2)),
      () -> assertEquals(ImmutableList.empty(), list.drop(10)),
      () -> assertEquals(listOf(4, 5, 6), listOf(1, 2, 3, 4, 5, 6).dropWhile(i -> i < 4)),
      () -> assertEquals(ImmutableList.empty(), listOf(1, 2, 3, 4, 5, 6).dropWhile(i -> true)),
      () -> assertEquals(listOf(1, 2, 3), listOf(1, 2, 3, 4, 5, 6).takeWhile(i -> i < 4)),
      () -> assertEquals(ImmutableList.empty(), listOf(1, 2, 3, 4, 5, 6).takeWhile(i -> false)),
      () -> assertEquals(listOf("a", "b", "c", "z"), list.append("z")),
      () -> assertEquals(listOf("a", "b"), list.remove("c")),
      () -> assertEquals(listOf("a", "b", "c"), list.remove("z")),
      () -> assertEquals(listOf("a", "b", "c", "z"), list.appendAll(listOf("z"))),
      () -> assertEquals(listOf("a", "b", "c"), list.map(identity())),
      () -> assertEquals(listOf("A", "B", "C"), list.map(toUpperCase)),
      () -> assertEquals(listOf("A", "B", "C"), list.flatMap(toUpperCase.sequence())),
      () -> assertEquals(listOf("a", "b", "c"), list.filter(e -> e.length() > 0)),
      () -> assertEquals(ImmutableList.empty(), list.filterNot(e -> e.length() > 0)),
      () -> assertEquals(ImmutableList.empty(), list.filter(e -> e.length() > 1)),
      () -> assertEquals(listOf("a", "b", "c"), list.filterNot(e -> e.length() > 1)),
      () -> assertEquals(list, list.stream().collect(toImmutableList())),
      () -> assertEquals(listOf(Tuple.of(0, "a"), Tuple.of(1, "b"), Tuple.of(2, "c")),
        list.zipWithIndex().collect(toImmutableList())),
      () -> assertThrows(UnsupportedOperationException.class, list.iterator()::remove)
    );
  }

  @Test
  public void emptyList() {
    ImmutableList<String> list = ImmutableList.empty();

    assertAll(
      () -> assertEquals(0, list.size()),
      () -> assertEquals(Option.none(), list.head()),
      () -> assertEquals(ImmutableList.empty(), list.tail()),
      () -> assertTrue(list.isEmpty()),
      () -> assertArrayEquals(new String[]{}, list.toArray(String[]::new)),
      () -> assertFalse(list.contains("z")),
      () -> assertFalse(list.containsAll(listOf("z"))),
      () -> assertEquals("", list.fold("", (a, b) -> a + b)),
      () -> assertEquals("", list.foldRight("", (a, b) -> a + b)),
      () -> assertEquals("", list.foldLeft("", (a, b) -> a + b)),
      () -> assertEquals(Option.none(), list.reduce((a, b) -> a + b)),
      () -> assertEquals(ImmutableList.empty(), list),
      () -> assertEquals(ImmutableList.from(Collections.emptyList()), list),
      () -> assertEquals(Collections.emptyList(), list.toList()),
      () -> assertEquals(ImmutableList.empty(), list.drop(1)),
      () -> assertEquals(listOf("z"), list.append("z")),
      () -> assertEquals(listOf("z"), list.appendAll(listOf("z"))),
      () -> assertEquals(ImmutableList.empty(), list.dropWhile(x -> true)),
      () -> assertEquals(ImmutableList.empty(), list.takeWhile(x -> false)),
      () -> assertEquals(ImmutableList.empty(), list.map(identity())),
      () -> assertEquals(ImmutableList.empty(), list.map(toUpperCase)),
      () -> assertEquals(ImmutableList.empty(), list.flatMap(toUpperCase.sequence())),
      () -> assertEquals(ImmutableList.empty(), list.filter(e -> e.length() > 1)),
      () -> assertEquals(ImmutableList.empty(), list.filterNot(e -> e.length() > 1))
    );
  }

  @Test
  void serialization() throws IOException, ClassNotFoundException {
    ImmutableList<Integer> list = listOf(1, 2, 3, 4, 5);

    var output = new ByteArrayOutputStream();
    try (var objectOutputStream = new ObjectOutputStream(output)) {
      objectOutputStream.writeObject(list);
      objectOutputStream.writeObject(Sequence.emptyList());
    }

    Object result = null;
    Object empty = null;
    try (var objectInputStream = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
      result = objectInputStream.readObject();
      empty = objectInputStream.readObject();
    }

    assertEquals(list, result);
    assertSame(Sequence.emptyList(), empty);
  }
}
