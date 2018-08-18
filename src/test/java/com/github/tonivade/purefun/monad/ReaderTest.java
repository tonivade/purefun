/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.monad.Reader.reader;
import static com.github.tonivade.purefun.monad.Reader.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.data.ImmutableList;

public class ReaderTest {

  @Test
  public void flatMap() {
    Reader<ImmutableList<String>, String> reader = begin("<")
        .flatMap(str -> read1(str))
        .flatMap(str -> read2(str))
        .flatMap(str -> read3(str))
        .flatMap(str -> end(str, ">"))
        .map(String::toUpperCase);

    assertEquals("<ABC>", reader.run(listOf("a", "b", "c")));
  }

  private Reader<ImmutableList<String>, String> begin(String str) {
    return unit(str);
  }

  private Reader<ImmutableList<String>, String> read1(String str) {
    return reader(list -> str + list.head().orElse(""));
  }

  private Reader<ImmutableList<String>, String> read2(String str) {
    return reader(list -> str + list.tail().head().orElse(""));
  }

  private Reader<ImmutableList<String>, String> read3(String str) {
    return reader(list -> str + list.tail().tail().head().orElse(""));
  }

  private Reader<ImmutableList<String>, String> end(String str, String end) {
    return reader(list -> str + end);
  }
}
