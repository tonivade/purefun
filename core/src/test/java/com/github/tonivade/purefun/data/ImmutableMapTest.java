/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.ImmutableMap.entry;
import static com.github.tonivade.purefun.data.Sequence.arrayOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.type.Option;

public class ImmutableMapTest {

  @Test
  public void nonEmptyMap() {
    ImmutableMap<String, String> map = ImmutableMap.of(entry("a", "aaa"),
                                                       entry("b", "bbb"),
                                                       entry("c", "ccc"));
    assertAll(() -> assertEquals(3, map.size()),
              () -> assertFalse(map.isEmpty()),
              () -> assertEquals(Option.some("aaa"), map.get("a")),
              () -> assertEquals(Option.none(), map.get("z")),
              () -> assertTrue(map.containsKey("a")),
              () -> assertFalse(map.containsKey("z")),
              () -> assertEquals(Option.some("aaa"), map.putIfAbsent("a", "zzz").get("a")),
              () -> assertEquals(Option.some("zzz"), map.putIfAbsent("z", "zzz").get("z")),
              () -> assertEquals("aaa", map.getOrDefault("a", () -> "zzz")),
              () -> assertEquals("zzz", map.getOrDefault("z", () -> "zzz")),
              () -> assertEquals(ImmutableSet.of("a", "b", "c"), map.keys()),
              () -> assertEquals(3, map.values().size()),
              () -> assertEquals(map.put("a", "aaaz"), map.merge("a", "z", (a, b) -> a + b)),
              () -> assertEquals(map.put("z", "a"), map.merge("z", "a", (a, b) -> a + b)),
              () -> assertTrue(map.values().contains("aaa")),
              () -> assertTrue(map.values().contains("bbb")),
              () -> assertTrue(map.values().contains("ccc")),
              () -> assertEquals(ImmutableMap.of(entry("c", "ccc")), map.remove("a").remove("b")),
              () -> assertEquals(ImmutableMap.of(entry("c", "ccc")), map.removeAll(arrayOf("a", "b"))),
              () -> assertEquals(ImmutableSet.of(entry("a", "aaa"),
                                                 entry("b", "bbb"),
                                                 entry("c", "ccc")), map.entries()),
              () -> assertEquals(map, ImmutableMap.of(entry("a", "aaa")).putAll(ImmutableMap.of(entry("b", "bbb"), entry("c", "ccc")))),
              () -> assertEquals(map, ImmutableMap.builder().put("a", "aaa").put("b", "bbb").put("c", "ccc").build())
              );
  }

  @Test
  public void empty() {
    ImmutableMap<String, String> map = ImmutableMap.empty();

    assertAll(() -> assertEquals(0, map.size()),
              () -> assertTrue(map.isEmpty()),
              () -> assertEquals(Option.none(), map.get("z")),
              () -> assertEquals("zzz", map.getOrDefault("a", () -> "zzz")),
              () -> assertEquals(ImmutableSet.empty(), map.keys()),
              () -> assertEquals(ImmutableList.empty(), map.values()),
              () -> assertEquals(ImmutableSet.empty(), map.entries()),
              () -> assertEquals(ImmutableMap.of(entry("a", "aaa")), map.put("a", "aaa")),
              () -> assertEquals(ImmutableMap.of(entry("A", "AAA")),
                                 map.put("a", "aaa").bimap(String::toUpperCase, String::toUpperCase)),
              () -> assertEquals(ImmutableMap.of(entry("A", "aaa")),
                                 map.put("a", "aaa").mapKeys(String::toUpperCase)),
              () -> assertEquals(ImmutableMap.of(entry("a", "AAA")),
                                 map.put("a", "aaa").mapValues(String::toUpperCase))
              );
  }

  @Test
  void serialization() throws IOException, ClassNotFoundException {
    ImmutableMap<Integer, String> map = ImmutableMap.of(entry(1, "uno"), entry(2, "dos"), entry(3, "tres"));

    var output = new ByteArrayOutputStream();
    try (var objectOutputStream = new ObjectOutputStream(output)) {
      objectOutputStream.writeObject(map);
      objectOutputStream.writeObject(ImmutableMap.empty());
    }

    Object result = null;
    Object empty = null;
    try (var objectInputStream = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
      result = objectInputStream.readObject();
      empty = objectInputStream.readObject();
    }

    assertEquals(map, result);
    assertSame(ImmutableMap.empty(), empty);
  }
}
