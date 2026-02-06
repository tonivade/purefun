/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.data.ImmutableArray.toImmutableArray;
import static com.github.tonivade.purefun.data.Sequence.arrayOf;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.data.Pipeline.chain;
import static com.github.tonivade.purefun.data.Pipeline.drop;
import static com.github.tonivade.purefun.data.Pipeline.dropWhile;
import static com.github.tonivade.purefun.data.Pipeline.filter;
import static com.github.tonivade.purefun.data.Pipeline.tumbling;
import static com.github.tonivade.purefun.data.Pipeline.map;
import static com.github.tonivade.purefun.data.Pipeline.sliding;
import static com.github.tonivade.purefun.data.Pipeline.take;
import static com.github.tonivade.purefun.data.Pipeline.takeWhile;
import static org.junit.jupiter.api.Assertions.assertAll;
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
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.type.Option;

public class ImmutableArrayTest {

  private final Function1<String, String> toUpperCase = String::toUpperCase;

  @Test
  public void notEmptyArray() {
    ImmutableArray<String> array = arrayOf("a", "b", "c");

    assertAll(() -> assertEquals(3, array.size()),
              () -> assertEquals("a", array.get(0)),
              () -> assertThrows(IndexOutOfBoundsException.class, () -> array.get(5)),
              () -> assertFalse(array.isEmpty()),
              () -> assertTrue(array.contains("a")),
              () -> assertFalse(array.contains("z")),
              () -> assertEquals("abc", array.fold("", (a, b) -> a + b)),
              () -> assertEquals("abc", array.foldLeft("", (a, b) -> a + b)),
              () -> assertEquals("abc", array.foldRight("", (a, b) -> a + b)),
              () -> assertEquals(Option.some("abc"), array.reduce((a, b) -> a + b)),
              () -> assertEquals(arrayOf("a", "b", "c"), array),
              () -> assertEquals(arrayOf("c", "b", "a"), array.reverse()),
              () -> assertEquals(arrayOf("c", "b", "a"), array.sort((a, b) -> b.compareTo(a))),
              () -> assertEquals(ImmutableArray.from(Arrays.asList("a", "b", "c")), array),
              () -> assertEquals(Arrays.asList("a", "b", "c"), array.toList()),
              () -> assertEquals(arrayOf("c"), array.drop(2)),
              () -> assertEquals(ImmutableArray.empty(), array.drop(10)),
              () -> assertEquals(arrayOf("a", "b", "z", "c"), array.insert(2, "z")),
              () -> assertEquals(arrayOf("a", "b", "z", "z", "c"), array.insertAll(2, arrayOf("z", "z"))),
              () -> assertEquals(arrayOf("a", "b", "z"), array.replace(2, "z")),
              () -> assertEquals(arrayOf("a", "c"), array.remove(1)),
              () -> assertEquals(arrayOf("a", "b", "c", "z"), array.append("z")),
              () -> assertEquals(arrayOf("a", "b"), array.remove("c")),
              () -> assertEquals(arrayOf("a", "b", "c"), array.remove("z")),
              () -> assertEquals(arrayOf("a", "b", "c", "z"), array.appendAll(arrayOf("z"))),
              () -> assertEquals(arrayOf("a", "b", "c"), array.map(identity())),
              () -> assertEquals(arrayOf("A", "B", "C"), array.map(toUpperCase)),
              () -> assertEquals(arrayOf("A", "B", "C"), array.run(chain(map(toUpperCase), filter(e -> e.length() > 0)))),
              () -> assertEquals(arrayOf("a", "b"), array.run(take(2))),
              () -> assertEquals(arrayOf("a", "b"), arrayOf("a", "b", "cc").run(takeWhile(e -> e.length() == 1))),
              () -> assertEquals(arrayOf("c"), array.run(drop(2))),
              () -> assertEquals(arrayOf("cc"), arrayOf("a", "b", "cc").run(dropWhile(e -> e.length() == 1))),
              () -> assertEquals(arrayOf(listOf("a", "b"), listOf("b", "c"), listOf("c", "d")), arrayOf("a", "b", "c", "d").run(sliding(2))),
              () -> assertEquals(arrayOf(listOf("a", "b"), listOf("c", "d")), arrayOf("a", "b", "c", "d").run(tumbling(2))),
              () -> assertEquals(arrayOf(listOf("a", "b"), listOf("c", "d")), arrayOf("a", "b", "c", "d", "e").run(tumbling(2))),
              () -> assertEquals(arrayOf("A", "B", "C"), array.flatMap(toUpperCase.sequence())),
              () -> assertEquals(arrayOf("a", "b", "c"), array.filter(e -> e.length() > 0)),
              () -> assertEquals(ImmutableArray.empty(), array.filter(e -> e.length() > 1)),
              () -> assertEquals(array, array.stream().collect(toImmutableArray())),
              () -> assertEquals(arrayOf(Tuple.of(0, "a"), Tuple.of(1, "b"), Tuple.of(2, "c")), array.zipWithIndex()),
              () -> assertThrows(UnsupportedOperationException.class, array.iterator()::remove)
              );
  }

  @Test
  public void emptyArray() {
    ImmutableArray<String> array = ImmutableArray.empty();

    assertAll(() -> assertEquals(0, array.size()),
              () -> assertThrows(IndexOutOfBoundsException.class, () -> array.get(5)),
              () -> assertTrue(array.isEmpty()),
              () -> assertFalse(array.contains("z")),
              () -> assertEquals("", array.fold("", (a, b) -> a + b)),
              () -> assertEquals("", array.foldRight("", (a, b) -> a + b)),
              () -> assertEquals("", array.foldLeft("", (a, b) -> a + b)),
              () -> assertEquals(Option.none(), array.reduce((a, b) -> a + b)),
              () -> assertEquals(ImmutableArray.empty(), array),
              () -> assertEquals(ImmutableArray.from(Collections.emptyList()), array),
              () -> assertEquals(Collections.emptyList(), array.toList()),
              () -> assertEquals(ImmutableArray.empty(), array.drop(1)),
              () -> assertEquals(arrayOf("z"), array.insert(0, "z")),
              () -> assertEquals(arrayOf("z", "z"), array.insertAll(0, arrayOf("z", "z"))),
              () -> assertThrows(IndexOutOfBoundsException.class, () -> array.insert(5, "z")),
              () -> assertThrows(IndexOutOfBoundsException.class, () -> array.replace(5, "z")),
              () -> assertThrows(IndexOutOfBoundsException.class, () -> array.remove(5)),
              () -> assertThrows(IndexOutOfBoundsException.class, () -> array.insertAll(5, arrayOf("z"))),
              () -> assertEquals(arrayOf("z"), array.append("z")),
              () -> assertEquals(arrayOf("z"), array.appendAll(arrayOf("z"))),
              () -> assertEquals(ImmutableArray.empty(), array.map(identity())),
              () -> assertEquals(ImmutableArray.empty(), array.map(toUpperCase)),
              () -> assertEquals(ImmutableArray.empty(), array.flatMap(toUpperCase.sequence())),
              () -> assertEquals(ImmutableArray.empty(), array.filter(e -> e.length() > 1))
              );
  }

  @Test
  void serialization() throws IOException, ClassNotFoundException {
    ImmutableArray<Integer> array = arrayOf(1, 2, 3, 4, 5);

    var output = new ByteArrayOutputStream();
    try (var objectOutputStream = new ObjectOutputStream(output)) {
      objectOutputStream.writeObject(array);
      objectOutputStream.writeObject(Sequence.emptyArray());
    }

    Object result = null;
    Object empty = null;
    try (var objectInputStream = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
      result = objectInputStream.readObject();
      empty = objectInputStream.readObject();
    }

    assertEquals(array, result);
    assertSame(Sequence.emptyArray(), empty);
  }
}
