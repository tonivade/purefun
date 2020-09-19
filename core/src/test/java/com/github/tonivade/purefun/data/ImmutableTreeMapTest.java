/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.ImmutableTreeMap.entry;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Option;

public class ImmutableTreeMapTest {

  @Test
  public void nonEmptyTree() {
    ImmutableTreeMap<String, String> treeMap = ImmutableTreeMap.of(entry("a", "aaa"),
                                                                   entry("b", "bbb"),
                                                                   entry("c", "ccc"));
    assertAll(() -> assertEquals(3, treeMap.size()),
              () -> assertFalse(treeMap.isEmpty()),
              () -> assertEquals(Option.some("aaa"), treeMap.get("a")),
              () -> assertEquals(Option.none(), treeMap.get("z")),
              () -> assertTrue(treeMap.containsKey("a")),
              () -> assertFalse(treeMap.containsKey("z")),
              () -> assertEquals(Option.some("aaa"), treeMap.putIfAbsent("a", "zzz").get("a")),
              () -> assertEquals(Option.some("zzz"), treeMap.putIfAbsent("z", "zzz").get("z")),
              () -> assertEquals("aaa", treeMap.getOrDefault("a", () -> "zzz")),
              () -> assertEquals("zzz", treeMap.getOrDefault("z", () -> "zzz")),
              () -> assertEquals(ImmutableSet.of("a", "b", "c"), treeMap.keys()),
              () -> assertEquals(3, treeMap.values().size()),
              () -> assertEquals(treeMap.put("a", "aaaz"), treeMap.merge("a", "z", (a, b) -> a + b)),
              () -> assertEquals(treeMap.put("z", "a"), treeMap.merge("z", "a", (a, b) -> a + b)),
              () -> assertTrue(treeMap.values().contains("aaa")),
              () -> assertTrue(treeMap.values().contains("bbb")),
              () -> assertTrue(treeMap.values().contains("ccc")),
              () -> assertEquals(ImmutableTreeMap.of(entry("c", "ccc")), treeMap.remove("a").remove("b")),
              () -> assertEquals(ImmutableSet.of(entry("a", "aaa"),
                                                 entry("b", "bbb"),
                                                 entry("c", "ccc")), treeMap.entries()),
              () -> assertEquals(Option.some("a"), treeMap.headKey()),
              () -> assertEquals(Option.some("c"), treeMap.tailKey()),
              () -> assertEquals(ImmutableTreeMap.of(entry("b", "bbb"),
                                                     entry("c", "ccc")), treeMap.tailMap("a")),
              () -> assertEquals(ImmutableTreeMap.of(entry("a", "aaa"),
                                                     entry("b", "bbb")), treeMap.headMap("c")),
              () -> assertEquals(Option.some("b"), treeMap.higherKey("a")),
              () -> assertEquals(Option.none(), treeMap.higherKey("c")),
              () -> assertEquals(Option.none(), treeMap.lowerKey("a")),
              () -> assertEquals(Option.some("b"), treeMap.lowerKey("c")),
              () -> assertEquals(Option.some("a"), treeMap.floorKey("a")),
              () -> assertEquals(Option.some("c"), treeMap.floorKey("c")),
              () -> assertEquals(Option.some("a"), treeMap.ceilingKey("a")),
              () -> assertEquals(Option.some("c"), treeMap.ceilingKey("c")),
              () -> assertEquals(ImmutableTreeMap.of(entry("a", "aaa"), entry("b", "bbb"), entry("c", "ccc")),
                                 ImmutableTreeMap.of(entry("a", "aaa")).putAll(ImmutableMap.of(entry("b", "bbb"),
                                                                                               entry("c", "ccc")))),
              () -> assertEquals(treeMap, ImmutableTreeMap.builder().put("a", "aaa").put("b", "bbb").put("c", "ccc").build())
              );
  }

  @Test
  public void empty() {
    ImmutableTreeMap<String, String> treeMap = ImmutableTreeMap.empty();

    assertAll(() -> assertEquals(0, treeMap.size()),
              () -> assertTrue(treeMap.isEmpty()),
              () -> assertEquals(Option.none(), treeMap.get("z")),
              () -> assertEquals("zzz", treeMap.getOrDefault("a", () -> "zzz")),
              () -> assertEquals(ImmutableSet.empty(), treeMap.keys()),
              () -> assertEquals(ImmutableList.empty(), treeMap.values()),
              () -> assertEquals(ImmutableSet.empty(), treeMap.entries()),
              () -> assertEquals(ImmutableTreeMap.of(entry("a", "aaa")), treeMap.put("a", "aaa")),
              () -> assertEquals(ImmutableTreeMap.of(entry("A", "AAA")),
                                 treeMap.put("a", "aaa").map(String::toUpperCase, String::toUpperCase)),
              () -> assertEquals(ImmutableTreeMap.of(entry("A", "aaa")),
                                 treeMap.put("a", "aaa").mapKeys(String::toUpperCase)),
              () -> assertEquals(ImmutableTreeMap.of(entry("a", "AAA")),
                                 treeMap.put("a", "aaa").mapValues(String::toUpperCase)),
              () -> assertEquals(Option.none(), treeMap.headKey()),
              () -> assertEquals(Option.none(), treeMap.tailKey()),
              () -> assertEquals(ImmutableTreeMap.empty(), treeMap.tailMap("a")),
              () -> assertEquals(ImmutableTreeMap.empty(), treeMap.headMap("c")),
              () -> assertEquals(Option.none(), treeMap.higherKey("a")),
              () -> assertEquals(Option.none(), treeMap.higherKey("c")),
              () -> assertEquals(Option.none(), treeMap.lowerKey("a")),
              () -> assertEquals(Option.none(), treeMap.lowerKey("c")),
              () -> assertEquals(Option.none(), treeMap.floorKey("a")),
              () -> assertEquals(Option.none(), treeMap.floorKey("c")),
              () -> assertEquals(Option.none(), treeMap.ceilingKey("a")),
              () -> assertEquals(Option.none(), treeMap.ceilingKey("c"))
              );
  }
}
