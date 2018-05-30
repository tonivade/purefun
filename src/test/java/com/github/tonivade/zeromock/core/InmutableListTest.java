package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Handler1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

public class InmutableListTest {
  
  final Handler1<String, String> toUpperCase = String::toUpperCase;
  
  @Test
  public void notEmptyList() {
    InmutableList<String> list = InmutableList.of("a", "b", "c");
    
    assertAll(() -> assertEquals(3, list.size()),
              () -> assertEquals(Option.some("a"), list.head()),
              () -> assertEquals(InmutableList.of("b", "c"), list.tail()),
              () -> assertTrue(list.contains("b")),
              () -> assertFalse(list.contains("z")),
              () -> assertFalse(list.isEmpty()),
              () -> assertEquals(InmutableList.of("a", "b", "c"), list),
              () -> assertEquals(InmutableList.from(Arrays.asList("a", "b", "c")), list),
              () -> assertEquals(Arrays.asList("a", "b", "c"), list.toList()),
              () -> assertEquals(InmutableList.of("c"), list.skip(2)),
              () -> assertEquals(InmutableList.empty(), list.skip(10)),
              () -> assertEquals(InmutableList.of("a", "b", "c", "z"), list.add("z")),
              () -> assertEquals(InmutableList.of("a", "b", "c", "z"), list.concat(InmutableList.of("z"))),
              () -> assertEquals(InmutableList.of("a", "b", "c"), list.map(identity())),
              () -> assertEquals(InmutableList.of("A", "B", "C"), list.map(toUpperCase)),
              () -> assertEquals(InmutableList.of("A", "B", "C"), list.flatMap(toUpperCase.liftSequence())),
              () -> assertEquals(InmutableList.of("a", "b", "c"), list.filter(e -> e.length() > 0)),
              () -> assertEquals(InmutableList.empty(), list.filter(e -> e.length() > 1)));
  }
  
  @Test
  public void emptyList() {
    InmutableList<String> list = InmutableList.empty();
    
    assertAll(() -> assertEquals(0, list.size()),
              () -> assertEquals(Option.none(), list.head()),
              () -> assertEquals(InmutableList.empty(), list.tail()),
              () -> assertFalse(list.contains("z")),
              () -> assertTrue(list.isEmpty()),
              () -> assertEquals(InmutableList.empty(), list),
              () -> assertEquals(InmutableList.from(Collections.emptyList()), list),
              () -> assertEquals(Collections.emptyList(), list.toList()),
              () -> assertEquals(InmutableList.empty(), list.skip(1)),
              () -> assertEquals(InmutableList.of("z"), list.add("z")),
              () -> assertEquals(InmutableList.of("z"), list.concat(InmutableList.of("z"))),
              () -> assertEquals(InmutableList.empty(), list.map(identity())),
              () -> assertEquals(InmutableList.empty(), list.map(toUpperCase)),
              () -> assertEquals(InmutableList.empty(), list.flatMap(toUpperCase.liftSequence())),
              () -> assertEquals(InmutableList.empty(), list.filter(e -> e.length() > 1)));
  }
}
