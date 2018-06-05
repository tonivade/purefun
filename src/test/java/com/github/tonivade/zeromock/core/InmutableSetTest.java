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

public class InmutableSetTest {
  
  private final Handler1<String, String> toUpperCase = String::toUpperCase;
  
  @Test
  public void notEmptySet() {
    InmutableSet<String> set = InmutableSet.of("a", "b", "c");
    
    assertAll(() -> assertEquals(3, set.size()),
              () -> assertFalse(set.isEmpty()),
              () -> assertTrue(set.contains("a")),
              () -> assertFalse(set.contains("z")),
              () -> assertEquals("abc", set.fold("", (a, b) -> a + b)),
              () -> assertEquals(Option.some("abc"), set.reduce((a, b) -> a + b)),
              () -> assertEquals(InmutableSet.of("a", "b", "c"), set),
              () -> assertEquals(InmutableSet.from(Arrays.asList("a", "b", "c")), set),
              () -> assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), set.toSet()),
              () -> assertEquals(InmutableSet.of("a", "b", "c"), set.append("c")),
              () -> assertEquals(InmutableSet.of("a", "b", "c", "z"), set.append("z")),
              () -> assertEquals(InmutableSet.of("a", "b"), set.remove("c")),
              () -> assertEquals(InmutableSet.of("a", "b", "c"), set.remove("z")),
              () -> assertEquals(InmutableSet.of("a", "b", "c", "z"), set.union(InmutableSet.of("z"))),
              () -> assertEquals(InmutableSet.empty(), set.intersection(InmutableSet.of("z"))),
              () -> assertEquals(InmutableSet.of("a", "b"), set.intersection(InmutableSet.of("a", "b"))),
              () -> assertEquals(InmutableSet.of("c"), set.difference(InmutableSet.of("a", "b"))),
              () -> assertEquals(InmutableSet.of("a", "b", "c"), set.map(identity())),
              () -> assertEquals(InmutableSet.of("A", "B", "C"), set.map(toUpperCase)),
              () -> assertEquals(InmutableSet.of("A", "B", "C"), set.flatMap(toUpperCase.liftSequence())),
              () -> assertEquals(InmutableSet.of("a", "b", "c"), set.filter(e -> e.length() > 0)),
              () -> assertEquals(InmutableSet.empty(), set.filter(e -> e.length() > 1)));
  }
  
  @Test
  public void emptyList() {
    InmutableSet<String> set = InmutableSet.empty();
    
    assertAll(() -> assertEquals(0, set.size()),
              () -> assertTrue(set.isEmpty()),
              () -> assertFalse(set.contains("z")),
              () -> assertEquals("", set.fold("", (a, b) -> a + b)),
              () -> assertEquals(Option.none(), set.reduce((a, b) -> a + b)),
              () -> assertEquals(InmutableSet.empty(), set),
              () -> assertEquals(InmutableSet.from(Collections.emptyList()), set),
              () -> assertEquals(Collections.emptySet(), set.toSet()),
              () -> assertEquals(InmutableSet.of("z"), set.append("z")),
              () -> assertEquals(InmutableSet.of("z"), set.union(InmutableSet.of("z"))),
              () -> assertEquals(InmutableSet.empty(), set.remove("c")),
              () -> assertEquals(InmutableSet.empty(), set.map(identity())),
              () -> assertEquals(InmutableSet.empty(), set.map(toUpperCase)),
              () -> assertEquals(InmutableSet.empty(), set.flatMap(toUpperCase.liftSequence())),
              () -> assertEquals(InmutableSet.empty(), set.filter(e -> e.length() > 1)));
  }
  
  @Test
  public void listLaws() {
    FunctorLaws.verifyLaws(InmutableSet.of("a", "b", "c"));
  }
}
