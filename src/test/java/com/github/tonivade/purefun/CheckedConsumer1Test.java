/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CheckedConsumer1Test {

  @Test
  public void andThen() throws Exception {
    List<String> strings = new LinkedList<>();

    CheckedConsumer1<String> add = CheckedConsumer1.of(strings::add);
    CheckedConsumer1<String> composite = add.andThen(add);
    composite.accept("value");

    assertEquals(asList("value", "value"), strings);
  }

  @Test
  public void peek() throws Throwable {
    List<String> strings = new LinkedList<>();

    CheckedConsumer1<String> add = CheckedConsumer1.of(strings::add);

    String string = add.peek().apply("value");

    assertAll(() -> assertEquals(asList("value"), strings),
              () -> assertEquals("value", string));
  }

  @Test
  public void asFunction() throws Throwable {
    List<String> strings = new LinkedList<>();

    CheckedConsumer1<String> add = CheckedConsumer1.of(strings::add);

    Nothing nothing = add.asFunction().apply("value");

    assertAll(() -> assertEquals(asList("value"), strings),
              () -> assertEquals(nothing(), nothing));
  }
}
