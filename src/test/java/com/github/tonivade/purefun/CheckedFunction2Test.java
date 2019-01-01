/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CheckedFunction2Test {

  private CheckedFunction2<String, String, String> concat = (a, b) -> a + b;
  private CheckedFunction2<Integer, Integer, Integer> sum = (a, b) -> a + b;
  private CheckedFunction1<String, Integer> str2int = str -> str.length();

  @Test
  public void curriedTest() throws Throwable {
    CheckedFunction1<String, CheckedFunction1<String, String>> handler = concat.curried();

    assertEquals("asdfg", handler.apply("asd").apply("fg"));
  }

  @Test
  public void tupledTest() throws Throwable {
    CheckedFunction1<Tuple2<String, String>, String> tupled = concat.tupled();

    assertEquals("asdfg", tupled.apply(Tuple2.of("asd", "fg")));
  }

  @Test
  public void andThenTest() throws Throwable {
    CheckedFunction2<String, String, Integer> handler = concat.andThen(str2int);

    assertEquals(Integer.valueOf(5), handler.apply("asd", "fg"));
  }

  @Test
  public void composeTest() throws Throwable {
    CheckedFunction1<String, Integer> handler = sum.compose(str2int, str2int);

    assertEquals(Integer.valueOf(10), handler.apply("asdfg"));
  }
}
