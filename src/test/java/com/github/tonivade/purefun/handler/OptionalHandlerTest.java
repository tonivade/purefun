/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class OptionalHandlerTest {
  
  @Test
  public void mapTest() {
    OptionalHandler<String, Integer> str2int = str -> Optional.of(str.length());
    
    assertEquals(Optional.of(10), str2int.map(a -> a * 2).apply("asdfg"));
  }
  
  @Test
  public void mapEmptyTest() {
    OptionalHandler<String, Integer> str2int = str -> Optional.empty();
    
    assertEquals(Optional.empty(), str2int.map(a -> a * 2).apply("asdfg"));
  }
  
  @Test
  public void orElseTest() {
    OptionalHandler<String, Integer> str2int = str -> Optional.empty();
    
    assertEquals(Integer.valueOf(0), str2int.orElse(() -> 0).apply("asdfg"));
  }
  
  @Test
  public void filterTest() {
    OptionalHandler<String, Integer> str2int = str -> Optional.of(str.length());
    
    assertEquals(Optional.of(5), str2int.filter(x -> x > 0).apply("asdfg"));
  }
  
  @Test
  public void filterEmptyTest() {
    OptionalHandler<String, Integer> str2int = str -> Optional.of(str.length());
    
    assertEquals(Optional.empty(), str2int.filter(x -> x > 10).apply("asdfg"));
  }
  
  @Test
  public void flatMapTest() {
    OptionalHandler<String, Integer> str2int = str -> Optional.of(str.length());
    
    assertEquals(Optional.of(10), str2int.flatMap(a -> Optional.of(a * 2)).apply("asdfg"));
  }
  
  @Test
  public void flatMapEmptyTest() {
    OptionalHandler<String, Integer> str2int = str -> Optional.of(str.length());
    
    assertEquals(Optional.empty(), str2int.flatMap(a -> Optional.empty()).apply("asdfg"));
  }

}
