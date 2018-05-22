/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class EitherHandlerTest {
  
  @Test
  public void mapRightRight() {
    EitherHandler<String, String, Integer> str2int = str -> Either.right(str.length());
    
    assertEquals(Either.right(10), str2int.map(i -> i * 2).handle("asdfg"));
  }
  
  @Test
  public void mapRightLeft() {
    EitherHandler<String, String, Integer> str2int = str -> Either.left("error");
    
    assertEquals(Either.left("error"), str2int.map(i -> i * 2).handle("asdfg"));
  }
  
  @Test
  public void mapLeftRight() {
    EitherHandler<String, String, Integer> str2int = str -> Either.right(str.length());
    
    assertEquals(Either.right(5), str2int.mapLeft(String::toUpperCase).handle("asdfg"));
  }
  
  @Test
  public void mapLeftLeft() {
    EitherHandler<String, String, Integer> str2int = str -> Either.left("error");
    
    assertEquals(Either.left("ERROR"), str2int.mapLeft(String::toUpperCase).handle("asdfg"));
  }
  
  @Test
  public void orElseRight() {
    EitherHandler<String, String, Integer> str2int = str -> Either.right(str.length());
    
    assertEquals(Either.right(5), str2int.orElse(() -> Either.right(50)).handle("asdfg"));
  }
  
  @Test
  public void orElseLeft() {
    EitherHandler<String, String, Integer> str2int = str -> Either.left("error");
    
    assertEquals(Either.right(50), str2int.orElse(() -> Either.right(50)).handle("asdfg"));
  }
  
  @Test
  public void filterRight() {
    EitherHandler<String, String, Integer> str2int = str -> Either.right(str.length());
    
    assertEquals(Option.some(Either.right(5)), str2int.filter(x -> x > 0).handle("asdfg"));
  }
  
  @Test
  public void notFilterRight() {
    EitherHandler<String, String, Integer> str2int = str -> Either.right(str.length());
    
    assertEquals(Option.none(), str2int.filter(x -> x > 10).handle("asdfg"));
  }
  
  @Test
  public void filterLeft() {
    EitherHandler<String, String, Integer> str2int = str -> Either.left("error");
    
    assertEquals(Option.none(), str2int.filter(x -> x > 0).handle("asdfg"));
  }
  
  @Test
  public void flatMapRight() {
    EitherHandler<String, String, Integer> str2int = str -> Either.right(str.length());
    
    assertEquals(Either.right(10), str2int.flatMap(a -> Either.right(a * 2)).handle("asdfg"));
  }
  
  @Test
  public void flatMapLeft() {
    EitherHandler<String, String, Integer> str2int = str -> Either.left("error");
    
    assertEquals(Either.left("error"), str2int.flatMap(a -> Either.right(a * 2)).handle("asdfg"));
  }
}
