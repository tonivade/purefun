package com.github.tonivade.purefun;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.StreamHandler;

public class StreamHandlerTest {
  
  @Test
  public void mapTest() {
    StreamHandler<String, String> handler = string -> Stream.of(string);
    Function1<String, Integer> str2int = string -> string.length();
    
    assertEquals(asList(5), handler.map(str2int).collect(toList()).apply("asdfg"));
  }
  
  @Test
  public void flatMapTest() {
    StreamHandler<String, String> handler = string -> Stream.of(string);
    StreamHandler<String, Integer> str2int = string -> Stream.of(string.length());
    
    assertEquals(asList(5), handler.flatMap(str2int).collect(toList()).apply("asdfg"));
  }
  
  @Test
  public void filterTest() {
    StreamHandler<String, String> handler = string -> Stream.of(string);
    
    assertEquals(asList("asdfg"), handler.filter(x -> x.length() > 0).collect(toList()).apply("asdfg"));
  }
  
  @Test
  public void filterEmptyTest() {
    StreamHandler<String, String> handler = string -> Stream.of(string);
    
    assertEquals(emptyList(), handler.filter(x -> x.length() > 5).collect(toList()).apply("asdfg"));
  }
}
