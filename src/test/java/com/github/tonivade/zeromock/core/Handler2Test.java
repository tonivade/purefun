/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Handler2.adapt;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

public class Handler2Test {
  
  private Handler2<String, String, String> concat = (a, b) -> a + b;
  private Handler2<Integer, Integer, Integer> sum = (a, b) -> a + b;
  private Handler1<String, Integer> str2int = str -> str.length();

  @Test
  public void curriedTest() {
    Handler1<String, Handler1<String, String>> handler = concat.curried();

    assertEquals("asdfg", handler.handle("asd").handle("fg"));
  }

  @Test
  public void andThenTest() {
    Handler2<String, String, Integer> handler = concat.andThen(str2int);

    assertEquals(Integer.valueOf(5), handler.handle("asd", "fg"));
  }

  @Test
  public void composeTest() {
    Handler1<String, Integer> handler = sum.compose(str2int, str2int);

    assertEquals(Integer.valueOf(10), handler.handle("asdfg"));
  }
  
  @Test
  public void adaptTest() {
    BiFunction<String, String, String> bifunction = (a, b) -> a + b;
    Handler2<String, String, String> handler = adapt(bifunction);

    assertEquals("asdfg", handler.handle("asd", "fg"));
  }
}
