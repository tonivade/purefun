/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.ImmutableList;
import com.github.tonivade.purefun.SequenceHandler;

public class SequenceHandlerTest {
  
  @Test
  public void mapTest() {
    SequenceHandler<String, String> handler = string -> listOf(string);
    Function1<String, Integer> str2int = string -> string.length();
    
    assertEquals(listOf(5), handler.map(str2int).apply("asdfg"));
  }
  
  @Test
  public void flatMapTest() {
    SequenceHandler<String, String> handler = string -> listOf(string);
    SequenceHandler<String, Integer> str2int = string -> listOf(string.length());
    
    assertEquals(listOf(5), handler.flatMap(str2int).apply("asdfg"));
  }
  
  @Test
  public void filterTest() {
    SequenceHandler<String, String> handler = string -> listOf(string);
    
    assertEquals(listOf("asdfg"), handler.filter(x -> x.length() > 0).apply("asdfg"));
  }
  
  @Test
  public void filterEmptyTest() {
    SequenceHandler<String, String> handler = string -> listOf(string);
    
    assertEquals(ImmutableList.empty(), handler.filter(x -> x.length() > 5).apply("asdfg"));
  }
}
