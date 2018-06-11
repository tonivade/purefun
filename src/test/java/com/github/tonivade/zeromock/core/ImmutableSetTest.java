package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Handler1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

public class ImmutableSetTest {
  
  private final Handler1<String, String> toUpperCase = String::toUpperCase;
  
  @Test
  public void notEmptySet() {
    ImmutableSet<String> set = ImmutableSet.of("a", "b", "c");
    
    assertAll(() -> assertEquals(3, set.size()),
              () -> assertFalse(set.isEmpty()),
              () -> assertTrue(set.contains("a")),
              () -> assertFalse(set.contains("z")),
              () -> assertEquals("abc", set.fold("", (a, b) -> a + b)),
              () -> assertEquals(Option.some("abc"), set.reduce((a, b) -> a + b)),
              () -> assertEquals(ImmutableSet.of("a", "b", "c"), set),
              () -> assertEquals(ImmutableSet.from(Arrays.asList("a", "b", "c")), set),
              () -> assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), set.toSet()),
              () -> assertEquals(ImmutableSet.of("a", "b", "c"), set.append("c")),
              () -> assertEquals(ImmutableSet.of("a", "b", "c", "z"), set.append("z")),
              () -> assertEquals(ImmutableSet.of("a", "b"), set.remove("c")),
              () -> assertEquals(ImmutableSet.of("a", "b", "c"), set.remove("z")),
              () -> assertEquals(ImmutableSet.of("a", "b", "c", "z"), set.union(ImmutableSet.of("z"))),
              () -> assertEquals(ImmutableSet.empty(), set.intersection(ImmutableSet.of("z"))),
              () -> assertEquals(ImmutableSet.of("a", "b"), set.intersection(ImmutableSet.of("a", "b"))),
              () -> assertEquals(ImmutableSet.of("c"), set.difference(ImmutableSet.of("a", "b"))),
              () -> assertEquals(ImmutableSet.of("a", "b", "c"), set.map(identity())),
              () -> assertEquals(ImmutableSet.of("A", "B", "C"), set.map(toUpperCase)),
              () -> assertEquals(ImmutableSet.of("A", "B", "C"), set.flatMap(toUpperCase.liftSequence())),
              () -> assertEquals(ImmutableSet.of("a", "b", "c"), set.filter(e -> e.length() > 0)),
              () -> assertEquals(ImmutableSet.empty(), set.filter(e -> e.length() > 1)));
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
              () -> assertEquals(Collections.emptySet(), set.toSet()),
              () -> assertEquals(ImmutableSet.of("z"), set.append("z")),
              () -> assertEquals(ImmutableSet.of("z"), set.union(ImmutableSet.of("z"))),
              () -> assertEquals(ImmutableSet.empty(), set.remove("c")),
              () -> assertEquals(ImmutableSet.empty(), set.map(identity())),
              () -> assertEquals(ImmutableSet.empty(), set.map(toUpperCase)),
              () -> assertEquals(ImmutableSet.empty(), set.flatMap(toUpperCase.liftSequence())),
              () -> assertEquals(ImmutableSet.empty(), set.filter(e -> e.length() > 1)));
  }
  
  @Test
  public void listLaws() {
    FunctorLaws.verifyLaws(ImmutableSet.of("a", "b", "c"));
  }
}
