/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.SequenceInstances;

public class WriterTest {

  @Test
  public void writerTest() {
    Writer<Sequence<String>, Integer> writer = WriterTest.<String, Integer>listPure(5)
        .flatMap(value -> listWriter("add 5", value + 5))
        .flatMap(value -> listWriter("plus 2", value * 2));

    assertAll(() -> assertEquals(Integer.valueOf(20), writer.getValue()),
              () -> assertEquals(listOf("add 5", "plus 2"), writer.getLog()));
  }

  private static <T, A> Writer<Sequence<T>, A> listPure(A value) {
    return Writer.pure(SequenceInstances.monoid(), value);
  }

  private static <T, A> Writer<Sequence<T>, A> listWriter(T log, A value) {
    return Writer.writer(SequenceInstances.monoid(), Tuple.of(listOf(log), value));
  }
}