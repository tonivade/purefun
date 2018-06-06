/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Handler1.adapt;
import static com.github.tonivade.zeromock.core.Handler1.identity;
import static com.github.tonivade.zeromock.core.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public class Handler1Test {
  
  private final Handler1<String, String> identity = identity();
  private final Handler1<String, Integer> str2int = string -> string.length();
  private final Handler1<Integer, String> int2str = integer -> String.valueOf(integer);
  
  @Test
  public void andThenTest() {
    String result = str2int.andThen(int2str).handle("asdfg");
    
    assertEquals("5", result);
  }
  
  @Test
  public void composeTest() {
    String result = int2str.compose(str2int).handle("asdfg");
    
    assertEquals("5", result);
  }
  
  @Test
  public void identityTest() {
    String result = identity.handle("5");
    
    assertEquals("5", result);
  }
  
  @Test
  public void liftOptionalTest() {
    Optional<Integer> result = str2int.liftOptional().handle("asdfg");

    assertEquals(Optional.of(5), result);
  }
  
  @Test
  public void liftOptionTest() {
    Option<Integer> result = str2int.liftOption().handle("asdfg");

    assertEquals(Option.some(5), result);
  }
  
  @Test
  public void liftTryTest() {
    Try<Integer> result = str2int.liftTry().handle("asdfg");

    assertEquals(Try.success(5), result);
  }
  
  @Test
  public void adaptSupplier() {
    Handler1<Nothing, String> handler = adapt(() -> "asdfg");

    assertEquals("asdfg", handler.handle(nothing()));
  }
  
  @Test
  public void adaptConsumer() {
    AtomicReference<String> reference = new AtomicReference<String>();
    Handler1<String, String> handler = adapt(reference::set);

    assertEquals("asdfg", handler.handle("asdfg"));
    assertEquals("asdfg", reference.get());
  }
  
  @Test
  public void adaptFunction() {
    Handler1<String, Integer> handler = adapt(str2int::handle);

    assertEquals(Integer.valueOf(5), handler.handle("asdfg"));
  }
}
