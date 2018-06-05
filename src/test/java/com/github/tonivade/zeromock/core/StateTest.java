/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StateTest {
  
  @Test
  public void concat() {
    State<InmutableList<String>, String> concat = State.<InmutableList<String>, String>unit("a")
        .flatMap(value -> new State<>(state -> Tupple2.of(value, state.append(value))));
    
    Tupple2<String, InmutableList<String>> run = concat.run(InmutableList.empty());
    
    assertEquals(Tupple2.of("a", InmutableList.of("a")), run);
  }
}
