/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Option;
import com.github.tonivade.purefun.Try;

public class Function1Test {
  
  private final Function1<String, String> identity = identity();
  private final Function1<String, Integer> str2int = string -> string.length();
  private final Function1<Integer, String> int2str = integer -> String.valueOf(integer);
  
  @Test
  public void andThenTest() {
    String result = str2int.andThen(int2str).apply("asdfg");
    
    assertEquals("5", result);
  }
  
  @Test
  public void composeTest() {
    String result = int2str.compose(str2int).apply("asdfg");
    
    assertEquals("5", result);
  }
  
  @Test
  public void identityTest() {
    String result = identity.apply("5");
    
    assertEquals("5", result);
  }
  
  @Test
  public void liftOptionalTest() {
    Optional<Integer> result = str2int.liftOptional().apply("asdfg");

    assertEquals(Optional.of(5), result);
  }
  
  @Test
  public void liftOptionTest() {
    Option<Integer> result = str2int.liftOption().apply("asdfg");

    assertEquals(Option.some(5), result);
  }
  
  @Test
  public void liftTryTest() {
    Try<Integer> result = str2int.liftTry().apply("asdfg");

    assertEquals(Try.success(5), result);
  }
}
