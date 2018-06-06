/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StateTest {
  
  @Test
  public void test() {
    State<InmutableList<String>, String> state = unit("a").flatMap(append("b")).flatMap(append("c"));
    
    Tupple2<InmutableList<String>, String> result = state.run(InmutableList.empty());
    
    assertEquals(Tupple2.of(InmutableList.of("a", "b"), "c"), result);
  }

  private State<InmutableList<String>, String> unit(String value) {
    return State.<InmutableList<String>, String>unit(value);
  }

  private Handler1<String, State<InmutableList<String>, String>> append(String nextVal) {
    return value -> new State<>(state -> Tupple2.of(state.append(value), nextVal));
  }
}
