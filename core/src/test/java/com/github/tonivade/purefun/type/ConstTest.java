/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ConstTest {

  @Test
  public void retag() {
    Const<String, Integer> val = Const.of("hello world!");

    Const<String, Float> retag = val.retag();

    assertEquals(retag, val);
    assertEquals(retag.get(), val.get());
  }
}
