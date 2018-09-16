/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PatternTest {
  
  @Test
  public void instanceOf() {
    Matcher<Object> isNumber = Matcher.instanceOf(Number.class);
    Matcher<Object> isString = Matcher.instanceOf(String.class);
    
    Pattern<Object, String> pattern = Pattern.<Object, String>build().when(isNumber).then(number -> "is number")
      .when(isString).then(string -> "is string")
      .when(Matcher.otherwise()).then(object -> "something else");
    
    
    assertAll(() -> assertEquals("is number", pattern.apply(1)),
        () -> assertEquals("is string", pattern.apply("1")),
        () -> assertEquals("something else", pattern.apply(null)));
  }
}
