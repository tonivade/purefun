/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TryHandlerTest {
  
  @Test
  public void mapTest() {
    TryHandler<String, Integer> str2int = str -> Try.success(str.length());
    
    assertEquals(Try.success(10), str2int.map(a -> a * 2).handle("asdfg"));
  }
  
  @Test
  public void mapEmptyTest() {
    TryHandler<String, Integer> str2int = str -> Try.failure("error");
    
    assertEquals(Try.failure("error").isFailure(), 
                 str2int.map(a -> a * 2).handle("asdfg").isFailure());
  }
  
  @Test
  public void orElseTest() {
    TryHandler<String, Integer> str2int = str -> Try.failure("error");
    
    assertEquals(Integer.valueOf(0), str2int.orElse(() -> 0).handle("asdfg"));
  }
  
  @Test
  public void filterTest() {
    TryHandler<String, Integer> str2int = str -> Try.success(str.length());
    
    assertEquals(Try.success(5), str2int.filter(x -> x > 0).handle("asdfg"));
  }
  
  @Test
  public void filterEmptyTest() {
    TryHandler<String, Integer> str2int = str -> Try.success(str.length());
    
    assertEquals(Try.failure("error").isFailure(), 
                 str2int.filter(x -> x > 10).handle("asdfg").isFailure());
  }
  
  @Test
  public void flatMapTest() {
    TryHandler<String, Integer> str2int = str -> Try.success(str.length());
    
    assertEquals(Try.success(10), str2int.flatMap(a -> Try.success(a * 2)).handle("asdfg"));
  }
  
  @Test
  public void flatMapEmptyTest() {
    TryHandler<String, Integer> str2int = str -> Try.success(str.length());
    
    assertEquals(Try.failure("error").isFailure(), 
                 str2int.flatMap(a -> Try.failure("error")).handle("asdfg").isFailure());
  }
  
  @Test
  public void recover() {
    TryHandler<String, Integer> str2int = str -> Try.failure("error");
    
    assertEquals(Try.success(5), str2int.recover(t -> 5).handle(null));
  }
}
