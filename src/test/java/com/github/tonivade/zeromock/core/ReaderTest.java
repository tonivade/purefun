/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Reader.reader;
import static com.github.tonivade.zeromock.core.Reader.unit;
import static com.github.tonivade.zeromock.core.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.zeromock.core.ImmutableList;
import com.github.tonivade.zeromock.core.Reader;

public class ReaderTest {
  
  @Test
  public void flatMap() {
    ImmutableList<String> list = listOf("a", "b", "c");
    
    String eval = begin("<")
        .flatMap(str -> read1(str))
        .flatMap(str -> read2(str))
        .flatMap(str -> read3(str))
        .flatMap(str -> end(str, ">"))
        .map(String::toUpperCase)
        .eval(list);
    
    assertEquals("<ABC>", eval);
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
