package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Handler1.identity;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

public class ImmutableListTest {
  
  private final Handler1<String, String> toUpperCase = String::toUpperCase;
  
  @Test
  public void notEmptyList() {
    ImmutableList<String> list = ImmutableList.of("a", "b", "c");
    
    assertAll(() -> assertEquals(3, list.size()),
              () -> assertEquals(Option.some("a"), list.head()),
              () -> assertEquals(ImmutableList.of("b", "c"), list.tail()),
              () -> assertFalse(list.isEmpty()),
              () -> assertTrue(list.contains("a")),
              () -> assertFalse(list.contains("z")),
              () -> assertEquals("abc", list.fold("", (a, b) -> a + b)),
              () -> assertEquals(Option.some("abc"), list.reduce((a, b) -> a + b)),
              () -> assertEquals(ImmutableList.of("a", "b", "c"), list),
              () -> assertEquals(ImmutableList.from(Arrays.asList("a", "b", "c")), list),
              () -> assertEquals(Arrays.asList("a", "b", "c"), list.toList()),
              () -> assertEquals(ImmutableList.of("c"), list.drop(2)),
              () -> assertEquals(ImmutableList.empty(), list.drop(10)),
              () -> assertEquals(ImmutableList.of("a", "b", "c", "z"), list.append("z")),
              () -> assertEquals(ImmutableList.of("a", "b", "c", "z"), list.appendAll(ImmutableList.of("z"))),
              () -> assertEquals(ImmutableList.of("a", "b", "c"), list.map(identity())),
              () -> assertEquals(ImmutableList.of("A", "B", "C"), list.map(toUpperCase)),
              () -> assertEquals(ImmutableList.of("A", "B", "C"), list.flatMap(toUpperCase.liftSequence())),
              () -> assertEquals(ImmutableList.of("a", "b", "c"), list.filter(e -> e.length() > 0)),
              () -> assertEquals(ImmutableList.empty(), list.filter(e -> e.length() > 1)));
  }
  
  @Test
  public void emptyList() {
    ImmutableList<String> list = ImmutableList.empty();
    
    assertAll(() -> assertEquals(0, list.size()),
              () -> assertEquals(Option.none(), list.head()),
              () -> assertEquals(ImmutableList.empty(), list.tail()),
              () -> assertTrue(list.isEmpty()),
              () -> assertFalse(list.contains("z")),
              () -> assertEquals("", list.fold("", (a, b) -> a + b)),
              () -> assertEquals(Option.none(), list.reduce((a, b) -> a + b)),
              () -> assertEquals(ImmutableList.empty(), list),
              () -> assertEquals(ImmutableList.from(Collections.emptyList()), list),
              () -> assertEquals(Collections.emptyList(), list.toList()),
              () -> assertEquals(ImmutableList.empty(), list.drop(1)),
              () -> assertEquals(ImmutableList.of("z"), list.append("z")),
              () -> assertEquals(ImmutableList.of("z"), list.appendAll(ImmutableList.of("z"))),
              () -> assertEquals(ImmutableList.empty(), list.map(identity())),
              () -> assertEquals(ImmutableList.empty(), list.map(toUpperCase)),
              () -> assertEquals(ImmutableList.empty(), list.flatMap(toUpperCase.liftSequence())),
              () -> assertEquals(ImmutableList.empty(), list.filter(e -> e.length() > 1)));
  }
  
  @Test
  public void listLaws() {
    FunctorLaws.verifyLaws(ImmutableList.of("a", "b", "c"));
  }
}
