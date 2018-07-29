/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.FunctorLaws;
import com.github.tonivade.purefun.type.Option;

public class ImmutableListTest {
  
  private final Function1<String, String> toUpperCase = String::toUpperCase;
  
  @Test
  public void notEmptyList() {
    ImmutableList<String> list = listOf("a", "b", "c");
    
    assertAll(() -> assertEquals(3, list.size()),
              () -> assertEquals(Option.some("a"), list.head()),
              () -> assertEquals(listOf("b", "c"), list.tail()),
              () -> assertFalse(list.isEmpty()),
              () -> assertTrue(list.contains("a")),
              () -> assertFalse(list.contains("z")),
              () -> assertEquals("abc", list.fold("", (a, b) -> a + b)),
              () -> assertEquals("abc", list.foldLeft("", (a, b) -> a + b)),
              () -> assertEquals("abc", list.foldRight("", (a, b) -> a + b)),
              () -> assertEquals(Option.some("abc"), list.reduce((a, b) -> a + b)),
              () -> assertEquals(listOf("a", "b", "c"), list),
              () -> assertEquals(listOf("c", "b", "a"), list.reverse()),
              () -> assertEquals(listOf("c", "b", "a"), list.sort((a, b) -> b.compareTo(a))),
              () -> assertEquals(ImmutableList.from(asList("a", "b", "c")), list),
              () -> assertEquals(asList("a", "b", "c"), list.toList()),
              () -> assertEquals(listOf("c"), list.drop(2)),
              () -> assertEquals(ImmutableList.empty(), list.drop(10)),
              () -> assertEquals(listOf("a", "b", "c", "z"), list.append("z")),
              () -> assertEquals(listOf("a", "b"), list.remove("c")),
              () -> assertEquals(listOf("a", "b", "c"), list.remove("z")),
              () -> assertEquals(listOf("a", "b", "c", "z"), list.appendAll(listOf("z"))),
              () -> assertEquals(listOf("a", "b", "c"), list.map(identity())),
              () -> assertEquals(listOf("A", "B", "C"), list.map(toUpperCase)),
              () -> assertEquals(listOf("A", "B", "C"), list.flatMap(toUpperCase.liftSequence())),
              () -> assertEquals(listOf("a", "b", "c"), listOf(list).flatten()),
              () -> assertThrows(UnsupportedOperationException.class, () -> list.flatten()),
              () -> assertEquals(listOf("a", "b", "c"), list.filter(e -> e.length() > 0)),
              () -> assertEquals(ImmutableList.empty(), list.filter(e -> e.length() > 1))
              );
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
              () -> assertEquals("", list.foldRight("", (a, b) -> a + b)),
              () -> assertEquals("", list.foldLeft("", (a, b) -> a + b)),
              () -> assertEquals(Option.none(), list.reduce((a, b) -> a + b)),
              () -> assertEquals(ImmutableList.empty(), list),
              () -> assertEquals(ImmutableList.from(Collections.emptyList()), list),
              () -> assertEquals(Collections.emptyList(), list.toList()),
              () -> assertEquals(ImmutableList.empty(), list.drop(1)),
              () -> assertEquals(listOf("z"), list.append("z")),
              () -> assertEquals(listOf("z"), list.appendAll(listOf("z"))),
              () -> assertEquals(ImmutableList.empty(), list.map(identity())),
              () -> assertEquals(ImmutableList.empty(), list.map(toUpperCase)),
              () -> assertEquals(ImmutableList.empty(), list.flatMap(toUpperCase.liftSequence())),
              () -> assertEquals(ImmutableList.empty(), listOf(list).flatten()),
              () -> assertEquals(ImmutableList.empty(), list.flatten()),
              () -> assertEquals(ImmutableList.empty(), list.filter(e -> e.length() > 1)));
  }
  
  @Test
  public void listLaws() {
    FunctorLaws.verifyLaws(listOf("a", "b", "c"));
  }
}
