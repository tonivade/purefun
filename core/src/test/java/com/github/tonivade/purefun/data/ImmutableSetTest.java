/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.data.ImmutableSet.toImmutableSet;
import static com.github.tonivade.purefun.data.Sequence.setOf;
import static java.util.Collections.emptySet;
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
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.type.Option;

public class ImmutableSetTest {

  private final Function1<String, String> toUpperCase = String::toUpperCase;

  @Test
  public void nonEmptySet() {
    ImmutableSet<String> set = setOf("a", "b", "c");

    assertAll(() -> assertEquals(3, set.size()),
              () -> assertFalse(set.isEmpty()),
              () -> assertTrue(set.contains("a")),
              () -> assertFalse(set.contains("z")),
              () -> assertEquals("abc", set.fold("", (a, b) -> a + b)),
              () -> assertEquals("cba", set.foldRight("", (a, b) -> a + b)),
              () -> assertEquals("abc", set.foldLeft("", (a, b) -> a + b)),
              () -> assertEquals(Option.some("abc"), set.reduce((a, b) -> a + b)),
              () -> assertEquals(setOf("a", "b", "c"), set),
              () -> assertEquals(ImmutableSet.from(Arrays.asList("a", "b", "c")), set),
              () -> assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), set.toSet()),
              () -> assertEquals(setOf("a", "b", "c"), set.append("c")),
              () -> assertEquals(setOf("a", "b", "c", "z"), set.append("z")),
              () -> assertEquals(setOf("a", "b"), set.remove("c")),
              () -> assertEquals(setOf("a", "b", "c"), set.remove("z")),
              () -> assertEquals(setOf("a", "b", "c", "z"), set.union(setOf("z"))),
              () -> assertEquals(ImmutableSet.empty(), set.intersection(setOf("z"))),
              () -> assertEquals(setOf("a", "b"), set.intersection(setOf("a", "b"))),
              () -> assertEquals(setOf("c"), set.difference(setOf("a", "b"))),
              () -> assertEquals(setOf("a", "b", "c"), set.map(identity())),
              () -> assertEquals(setOf("A", "B", "C"), set.map(toUpperCase)),
              () -> assertEquals(setOf("A", "B", "C"), set.flatMap(toUpperCase.sequence())),
              () -> assertEquals(setOf("a", "b", "c"), set.filter(e -> e.length() > 0)),
              () -> assertEquals(ImmutableSet.empty(), set.filter(e -> e.length() > 1)),
              () -> assertEquals(set, set.stream().collect(toImmutableSet())),
              () -> assertEquals(setOf(Tuple.of(0, "a"), Tuple.of(1, "b"), Tuple.of(2, "c")),
                  set.zipWithIndex().collect(toImmutableSet())),
              () -> assertThrows(UnsupportedOperationException.class, set.iterator()::remove)
              );
  }

  @Test
  public void emptyList() {
    ImmutableSet<String> set = ImmutableSet.empty();

    assertAll(() -> assertEquals(0, set.size()),
              () -> assertTrue(set.isEmpty()),
              () -> assertFalse(set.contains("z")),
              () -> assertEquals("", set.fold("", (a, b) -> a + b)),
              () -> assertEquals(Option.none(), set.reduce((a, b) -> a + b)),
              () -> assertEquals(ImmutableSet.empty(), set),
              () -> assertEquals(ImmutableSet.from(Collections.emptyList()), set),
              () -> assertEquals(emptySet(), set.toSet()),
              () -> assertEquals(setOf("z"), set.append("z")),
              () -> assertEquals(setOf("z"), set.union(setOf("z"))),
              () -> assertEquals(ImmutableSet.empty(), set.remove("c")),
              () -> assertEquals(ImmutableSet.empty(), set.map(identity())),
              () -> assertEquals(ImmutableSet.empty(), set.map(toUpperCase)),
              () -> assertEquals(ImmutableSet.empty(), set.flatMap(toUpperCase.sequence())),
              () -> assertEquals(ImmutableSet.empty(), set.filter(e -> e.length() > 1)));
  }
  
  @Test
  void serialization() throws IOException, ClassNotFoundException {
    ImmutableSet<Integer> set = setOf(1, 2, 3, 4, 5);

    var output = new ByteArrayOutputStream();
    try (var objectOutputStream = new ObjectOutputStream(output)) {
      objectOutputStream.writeObject(set);
      objectOutputStream.writeObject(Sequence.emptySet());
    }
    
    Object result = null;
    Object empty = null;
    try (var objectInputStream = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
      result = objectInputStream.readObject();
      empty = objectInputStream.readObject();
    }
    
    assertEquals(set, result);
    assertSame(Sequence.emptySet(), empty);
  }
}
