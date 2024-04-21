/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.core.Unit.unit;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Unit;

public class Consumer1Test {

  @Test
  public void andThen() {
    List<String> strings = new LinkedList<>();

    Consumer1<String> add = Consumer1.of(strings::add);
    Consumer1<String> composite = add.andThen(add);
    composite.accept("value");

    assertEquals(asList("value", "value"), strings);
  }

  @Test
  public void peek() {
    List<String> strings = new LinkedList<>();

    Consumer1<String> add = Consumer1.of(strings::add);

    String string = add.peek().apply("value");

    assertAll(() -> assertEquals(asList("value"), strings),
              () -> assertEquals("value", string));
  }

  @Test
  public void asFunction() {
    List<String> strings = new LinkedList<>();

    Consumer1<String> add = Consumer1.of(strings::add);

    Unit unit = add.asFunction().apply("value");

    assertAll(() -> assertEquals(asList("value"), strings),
              () -> assertEquals(unit(), unit));
  }
}
