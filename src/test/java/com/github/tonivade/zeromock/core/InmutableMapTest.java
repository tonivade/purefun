/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.InmutableMap.entry;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class InmutableMapTest {

  @Test
  public void nonEmptyMap() {
    InmutableMap<String, String> map = InmutableMap.of(entry("a", "aaa"), 
                                                       entry("b", "bbb"),
                                                       entry("c", "ccc"));
    assertAll(() -> assertEquals(3, map.size()),
              () -> assertFalse(map.isEmpty()),
              () -> assertEquals(Option.some("aaa"), map.get("a")),
              () -> assertEquals(Option.none(), map.get("z")),
              () -> assertEquals("aaa", map.getOrDefault("a", () -> "zzz")),
              () -> assertEquals("zzz", map.getOrDefault("z", () -> "zzz")),
              () -> assertEquals(InmutableSet.of("a", "b", "c"), map.keys()),
              () -> assertEquals(3, map.values().size()),
              () -> assertTrue(map.values().contains("aaa")),
              () -> assertTrue(map.values().contains("bbb")),
              () -> assertTrue(map.values().contains("ccc")),
              () -> assertEquals(InmutableSet.of(entry("a", "aaa"), 
                                                 entry("b", "bbb"), 
                                                 entry("c", "ccc")), map.entries())
              );
  }

  @Test
  public void empty() {
    InmutableMap<String, String> map = InmutableMap.empty();
    
    assertAll(() -> assertEquals(0, map.size()),
              () -> assertTrue(map.isEmpty()),
              () -> assertEquals(Option.none(), map.get("z")),
              () -> assertEquals("zzz", map.getOrDefault("a", () -> "zzz")),
              () -> assertEquals(InmutableSet.empty(), map.keys()),
              () -> assertEquals(InmutableList.empty(), map.values()),
              () -> assertEquals(InmutableSet.empty(), map.entries())
              );
  }
}
