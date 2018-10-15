/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.ImmutableTreeMap.entry;
import static com.github.tonivade.purefun.data.Sequence.setOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Option;

public class ImmutableTreeMapTest {

  @Test
  public void nonEmptyTree() {
    ImmutableTreeMap<String, String> map = ImmutableTreeMap.of(entry("a", "aaa"),
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
              () -> assertEquals(ImmutableTreeMap.of(entry("c", "ccc")), map.remove("a").remove("b")),
              () -> assertEquals(ImmutableSet.of(entry("a", "aaa"),
                                                 entry("b", "bbb"),
                                                 entry("c", "ccc")), map.entries()),
              () -> assertEquals(Option.some("a"), map.headKey()),
              () -> assertEquals(Option.some("c"), map.tailKey()),
              () -> assertEquals(ImmutableTreeMap.of(entry("b", "bbb"),
                                                     entry("c", "ccc")), map.tailMap("a")),
              () -> assertEquals(ImmutableTreeMap.of(entry("a", "aaa"),
                                                     entry("b", "bbb")), map.headMap("c")),
              () -> assertEquals(Option.some("b"), map.higherKey("a")),
              () -> assertEquals(Option.none(), map.higherKey("c")),
              () -> assertEquals(Option.none(), map.lowerKey("a")),
              () -> assertEquals(Option.some("b"), map.lowerKey("c")),
              () -> assertEquals(Option.some("a"), map.floorKey("a")),
              () -> assertEquals(Option.some("c"), map.floorKey("c")),
              () -> assertEquals(Option.some("a"), map.ceilingKey("a")),
              () -> assertEquals(Option.some("c"), map.ceilingKey("c")),
              () -> assertEquals(ImmutableTreeMap.of(entry("a", "aaa"), entry("b", "bbb"), entry("c", "ccc")),
                                 ImmutableTreeMap.of(entry("a", "aaa")).putAll(setOf(entry("b", "bbb"),
                                                                                     entry("c", "ccc")))),
              () -> assertEquals(map, ImmutableTreeMap.builder().put("a", "aaa").put("b", "bbb").put("c", "ccc").build())
              );
  }

  @Test
  public void empty() {
    ImmutableTreeMap<String, String> map = ImmutableTreeMap.empty();

    assertAll(() -> assertEquals(0, map.size()),
              () -> assertTrue(map.isEmpty()),
              () -> assertEquals(Option.none(), map.get("z")),
              () -> assertEquals("zzz", map.getOrDefault("a", () -> "zzz")),
              () -> assertEquals(ImmutableSet.empty(), map.keys()),
              () -> assertEquals(ImmutableList.empty(), map.values()),
              () -> assertEquals(ImmutableSet.empty(), map.entries()),
              () -> assertEquals(ImmutableTreeMap.of(entry("a", "aaa")), map.put("a", "aaa")),
              () -> assertEquals(ImmutableTreeMap.of(entry("A", "AAA")),
                                 map.put("a", "aaa").map(String::toUpperCase, String::toUpperCase)),
              () -> assertEquals(ImmutableTreeMap.of(entry("A", "aaa")),
                                 map.put("a", "aaa").mapKeys(String::toUpperCase)),
              () -> assertEquals(ImmutableTreeMap.of(entry("a", "AAA")),
                                 map.put("a", "aaa").mapValues(String::toUpperCase)),
              () -> assertEquals(Option.none(), map.headKey()),
              () -> assertEquals(Option.none(), map.tailKey()),
              () -> assertEquals(ImmutableTreeMap.empty(), map.tailMap("a")),
              () -> assertEquals(ImmutableTreeMap.empty(), map.headMap("c")),
              () -> assertEquals(Option.none(), map.higherKey("a")),
              () -> assertEquals(Option.none(), map.higherKey("c")),
              () -> assertEquals(Option.none(), map.lowerKey("a")),
              () -> assertEquals(Option.none(), map.lowerKey("c")),
              () -> assertEquals(Option.none(), map.floorKey("a")),
              () -> assertEquals(Option.none(), map.floorKey("c")),
              () -> assertEquals(Option.none(), map.ceilingKey("a")),
              () -> assertEquals(Option.none(), map.ceilingKey("c"))
              );
  }
}
