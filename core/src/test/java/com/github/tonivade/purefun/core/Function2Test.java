/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class Function2Test {

  private Function2<String, String, String> concat = (a, b) -> a + b;
  private Function2<Integer, Integer, Integer> sum = (a, b) -> a + b;
  private Function1<String, Integer> str2int = str -> str.length();

  @Test
  public void curriedTest() {
    Function1<String, Function1<String, String>> handler = concat.curried();

    assertEquals("asdfg", handler.apply("asd").apply("fg"));
  }

  @Test
  public void tupledTest() {
    Function1<Tuple2<String, String>, String> tupled = concat.tupled();

    assertEquals("asdfg", tupled.apply(Tuple2.of("asd", "fg")));
  }

  @Test
  public void andThenTest() {
    Function2<String, String, Integer> handler = concat.andThen(str2int);

    assertEquals(Integer.valueOf(5), handler.apply("asd", "fg"));
  }

  @Test
  public void composeTest() {
    Function1<String, Integer> handler = sum.compose(str2int, str2int);

    assertEquals(Integer.valueOf(10), handler.apply("asdfg"));
  }
}
