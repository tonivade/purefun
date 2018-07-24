/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.ImmutableMap.entry;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.ImmutableList;
import com.github.tonivade.purefun.ImmutableMap;
import com.github.tonivade.purefun.ImmutableSet;
import com.github.tonivade.purefun.Option;

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
              () -> assertEquals(ImmutableSet.of(entry("a", "aaa"), 
                                                 entry("b", "bbb"), 
                                                 entry("c", "ccc")), map.entries())
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
              () -> assertEquals(ImmutableMap.of(entry("A", "aaa")), 
                                 map.put("a", "aaa").mapKeys(String::toUpperCase)),
              () -> assertEquals(ImmutableMap.of(entry("a", "AAA")), 
                                 map.put("a", "aaa").mapValues(String::toUpperCase))
              );
  }
}
