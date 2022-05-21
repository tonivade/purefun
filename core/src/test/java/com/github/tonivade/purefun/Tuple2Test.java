/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class Tuple2Test {
  
  @Test
  public void tuple() {
    Tuple2<String, Integer> tuple = Tuple.of("value", 10);

    assertAll(() -> assertEquals(Tuple.of("value", 10), tuple),
              () -> assertEquals(Tuple.of("VALUE", 10), tuple.map1(String::toUpperCase)),
              () -> assertEquals(Tuple.of("value", 100), tuple.map2(i -> i * i)),
              () -> assertEquals(Tuple.of("VALUE", 100), tuple.map(String::toUpperCase, i -> i * i)),
              () -> assertEquals("value", tuple.get1()),
              () -> assertEquals(Integer.valueOf(10), tuple.get2()),
              () -> assertEquals("Tuple2(value, 10)", tuple.toString())
        );
  }
}
