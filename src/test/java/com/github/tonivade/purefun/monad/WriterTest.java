/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Monoid;
import com.github.tonivade.purefun.data.ImmutableList;

public class WriterTest {

  private final Monoid<ImmutableList<String>> monoid = new ListStringMonoid();

  @Test
  public void writerTest() {
    Writer<ImmutableList<String>, Integer> writer = Writer.pure(monoid, 5)
        .flatMap(value -> new Writer<>(monoid, listOf("add 5"), value + 5))
        .flatMap(value -> new Writer<>(monoid, listOf("plus 2"), value * 2));

    assertAll(() -> assertEquals(Integer.valueOf(20), writer.getValue()),
              () -> assertEquals(listOf("add 5", "plus 2"), writer.getLog()));

  }
}

class ListStringMonoid implements Monoid<ImmutableList<String>> {

  @Override
  public ImmutableList<String> zero() {
    return ImmutableList.empty();
  }

  @Override
  public ImmutableList<String> combine(ImmutableList<String> t1, ImmutableList<String> t2) {
    return t1.appendAll(t2);
  }
}
