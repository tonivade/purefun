/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Combinators.flatMap;
import static com.github.tonivade.zeromock.core.Combinators.force;
import static com.github.tonivade.zeromock.core.Combinators.identity;
import static com.github.tonivade.zeromock.core.Combinators.join;
import static com.github.tonivade.zeromock.core.Combinators.map;
import static com.github.tonivade.zeromock.core.Combinators.orElse;
import static com.github.tonivade.zeromock.core.Combinators.split;
import static com.github.tonivade.zeromock.core.Requests.get;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.tonivade.zeromock.core.Combinators.BiTupple;

public class CombinatorsTest {
  @Test
  public void identityTest() {
    assertEquals(get("/path"), identity().apply(get("/path")));
  }

  @Test
  public void consumer() {
    StringBuffer buffer = new StringBuffer();

    force(value -> buffer.append(value)).apply("value");

    assertEquals("value", buffer.toString());
  }
  
  @Test
  public void supplier() {
    String value = force(() -> "value").apply("xxx");

    assertEquals("value", value);
  }
  
  @Test
  public void joinTest() {
    BiTupple<String, String> tupple = join(force(() -> "a"), force(() -> "b")).apply("xxx");
    
    assertAll(() -> assertEquals("a", tupple.get1()),
              () -> assertEquals("b", tupple.get2()));
  }
  
  @Test
  public void splitTest() {
    String string = split((String a, String b) -> a + b).apply(BiTupple.of("a", "b"));
    
    assertEquals("ab", string);
  }
  
  @Test
  public void mapNotEmpty() {
    Optional<Integer> optional = map((String a) -> a.length()).apply(Optional.of("asdfg"));
    
    assertEquals(Optional.of(5), optional);
  }
  
  @Test
  public void mapEmpty() {
    Optional<Integer> optional = map((String a) -> a.length()).apply(Optional.empty());
    
    assertEquals(Optional.empty(), optional);
  }
  
  @Test
  public void orElseNotEmpty() {
    String string = orElse(() -> "not found").apply(Optional.of("not empty"));
    
    assertEquals("not empty", string);
  }
  
  @Test
  public void orElseEmpty() {
    String string = orElse(() -> "not found").apply(Optional.empty());
    
    assertEquals("not found", string);
  }
  
  @Test
  public void flatMapNotEmpty() {
    Optional<Integer> optional = flatMap((String a) -> Optional.of(a.length())).apply(Optional.of("asdfg"));
    
    assertEquals(Optional.of(5), optional);
  }
  
  @Test
  public void flatMapEmpty() {
    Optional<Integer> optional = flatMap((String a) -> Optional.of(a.length())).apply(Optional.empty());
    
    assertEquals(Optional.empty(), optional);
  }
  
  @Test
  public void flatMapNotEmptyAndEmpty() {
    Optional<Object> optional = flatMap((String a) -> Optional.empty()).apply(Optional.of("asdf"));
    
    assertEquals(Optional.empty(), optional);
  }
}
