/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Pattern3;

public class Pattern3Test {

  @Test
  public void pattern3() {
    Pattern3<Object, Object, Object, Object> pattern3 = Pattern3.build()
      .when((a, b, c) -> a.equals(b) && b.equals(c) && a.equals(c)).returns("a equals b equals c")
      .otherwise().returns("not equals");

    assertAll(() -> assertEquals("a equals b equals c", pattern3.apply("a", "a", "a")),
              () -> assertEquals("not equals", pattern3.apply("a", 12L, 2.2)));
  }
}
