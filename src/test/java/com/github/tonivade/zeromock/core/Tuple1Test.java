/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class Tuple1Test {
  
  @Test
  public void tuple() {
    Tuple1<String> tuple = Tuple.of("value");

    assertAll(() -> assertEquals(Tuple.of("value"), tuple),
              () -> assertEquals(Tuple.of("VALUE"), tuple.map(String::toUpperCase)),
              () -> assertEquals("value", tuple.get1()),
              () -> assertEquals("Tuple1(value)", tuple.toString())
        );
  }
}
