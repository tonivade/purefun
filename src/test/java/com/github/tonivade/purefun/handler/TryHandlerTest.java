/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Try;

public class TryHandlerTest {

  @Test
  public void mapTest() {
    TryHandler<String, Integer> str2int = str -> Try.success(str.length());

    assertEquals(Try.success(10), str2int.map(a -> a * 2).apply("asdfg"));
  }

  @Test
  public void mapEmptyTest() {
    TryHandler<String, Integer> str2int = str -> Try.failure("error");

    assertEquals(Try.failure("error").isFailure(),
                 str2int.map(a -> a * 2).applyK("asdfg").isFailure());
  }

  @Test
  public void orElseTest() {
    TryHandler<String, Integer> str2int = str -> Try.failure("error");

    assertEquals(Integer.valueOf(0), str2int.orElse(0).apply("asdfg"));
  }

  @Test
  public void filterTest() {
    TryHandler<String, Integer> str2int = str -> Try.success(str.length());

    assertEquals(Try.success(5), str2int.filter(x -> x > 0).apply("asdfg"));
  }

  @Test
  public void filterEmptyTest() {
    TryHandler<String, Integer> str2int = str -> Try.success(str.length());

    assertEquals(Try.failure("error").isFailure(),
                 str2int.filter(x -> x > 10).applyK("asdfg").isFailure());
  }

  @Test
  public void flatMapTest() {
    TryHandler<String, Integer> str2int = str -> Try.success(str.length());

    assertEquals(Try.success(10), str2int.flatMap(a -> Try.success(a * 2)).apply("asdfg"));
  }

  @Test
  public void flatMapEmptyTest() {
    TryHandler<String, Integer> str2int = str -> Try.success(str.length());

    assertEquals(Try.failure("error").isFailure(),
                 str2int.flatMap(a -> Try.failure("error")).applyK("asdfg").isFailure());
  }

  @Test
  public void recover() {
    TryHandler<String, Integer> str2int = str -> Try.failure("error");

    assertEquals(Try.success(5), str2int.recover(t -> 5).apply(null));
  }
}
