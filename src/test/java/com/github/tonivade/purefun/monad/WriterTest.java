/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.monad.Writer.listWriter;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.data.Sequence;

public class WriterTest {

  @Test
  public void writerTest() {
    Writer<Sequence<String>, Integer> writer = Writer.<String, Integer>listPure(5)
        .flatMap(value -> listWriter("add 5", value + 5))
        .flatMap(value -> listWriter("plus 2", value * 2));

    assertAll(() -> assertEquals(Integer.valueOf(20), writer.getValue()),
              () -> assertEquals(listOf("add 5", "plus 2"), writer.getLog()));
  }
}