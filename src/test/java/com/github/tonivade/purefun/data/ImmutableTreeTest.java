/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.ImmutableTree.entry;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Option;

public class ImmutableTreeTest {

  @Test
  public void nonEmptyTree() {
    ImmutableTree<String, String> tree = ImmutableTree.of(entry("a", "aaa"),
                                                         entry("b", "bbb"),
                                                         entry("c", "ccc"));
    assertAll(() -> assertEquals(3, tree.size()),
              () -> assertFalse(tree.isEmpty()),
              () -> assertEquals(Option.some("aaa"), tree.get("a")),
              () -> assertEquals(Option.none(), tree.get("z")),
              () -> assertTrue(tree.containsKey("a")),
              () -> assertFalse(tree.containsKey("z")),
              () -> assertEquals(Option.some("aaa"), tree.putIfAbsent("a", "zzz").get("a")),
              () -> assertEquals(Option.some("zzz"), tree.putIfAbsent("z", "zzz").get("z")),
              () -> assertEquals("aaa", tree.getOrDefault("a", () -> "zzz")),
              () -> assertEquals("zzz", tree.getOrDefault("z", () -> "zzz")),
              () -> assertEquals(ImmutableSet.of("a", "b", "c"), tree.keys()),
              () -> assertEquals(3, tree.values().size()),
              () -> assertEquals(tree.put("a", "aaaz"), tree.merge("a", "z", (a, b) -> a + b)),
              () -> assertEquals(tree.put("z", "a"), tree.merge("z", "a", (a, b) -> a + b)),
              () -> assertTrue(tree.values().contains("aaa")),
              () -> assertTrue(tree.values().contains("bbb")),
              () -> assertTrue(tree.values().contains("ccc")),
              () -> assertEquals(ImmutableTree.of(entry("c", "ccc")), tree.remove("a").remove("b")),
              () -> assertEquals(ImmutableSet.of(entry("a", "aaa"),
                                                 entry("b", "bbb"),
                                                 entry("c", "ccc")), tree.entries())
              );
  }

  @Test
  public void empty() {
    ImmutableTree<String, String> tree = ImmutableTree.empty();

    assertAll(() -> assertEquals(0, tree.size()),
              () -> assertTrue(tree.isEmpty()),
              () -> assertEquals(Option.none(), tree.get("z")),
              () -> assertEquals("zzz", tree.getOrDefault("a", () -> "zzz")),
              () -> assertEquals(ImmutableSet.empty(), tree.keys()),
              () -> assertEquals(ImmutableList.empty(), tree.values()),
              () -> assertEquals(ImmutableSet.empty(), tree.entries()),
              () -> assertEquals(ImmutableTree.of(entry("a", "aaa")), tree.put("a", "aaa")),
              () -> assertEquals(ImmutableTree.of(entry("A", "AAA")),
                                 tree.put("a", "aaa").map(String::toUpperCase, String::toUpperCase)),
              () -> assertEquals(ImmutableTree.of(entry("A", "aaa")),
                                 tree.put("a", "aaa").mapKeys(String::toUpperCase)),
              () -> assertEquals(ImmutableTree.of(entry("a", "AAA")),
                                 tree.put("a", "aaa").mapValues(String::toUpperCase))
              );
  }
}
