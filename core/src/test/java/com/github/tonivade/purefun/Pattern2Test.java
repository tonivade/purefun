/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class Pattern2Test {

  @Test
  public void pattern2() {
    Pattern2<Object, Object, Object> pattern2 = Pattern2.build()
      .when(Object::equals).returns("a equals b")
      .otherwise().returns("not equals");
    
    assertAll(() -> assertEquals("a equals b", pattern2.apply("a", "a")),
              () -> assertEquals("not equals", pattern2.apply("a", 12L)));
  }
}
