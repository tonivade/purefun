/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Unit.unit;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class Consumer2Test {

  @Test
  public void andThen() {
    List<String> strings = new LinkedList<>();

    Consumer2<String, Integer> add = (a, b) -> strings.add(a + "=" + b);
    Consumer2<String, Integer> composite = add.andThen(add);
    composite.accept("value", 100);

    assertEquals(asList("value=100", "value=100"), strings);
  }

  @Test
  public void asFunction() {
    List<String> strings = new LinkedList<>();

    Consumer2<String, Integer> add = (a, b) -> strings.add(a + "=" + b);
    Unit unit = add.asFunction().apply("value", 100);

    assertAll(() -> assertEquals(asList("value=100"), strings),
              () -> assertEquals(unit(), unit));
  }
}
