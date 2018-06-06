/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Nothing.nothing;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StateTest {
  
  @Test
  public void get() {
    assertEquals(Tupple2.of("abc", "abc"), State.get().run("abc"));
  }
  
  @Test
  public void set() {
    assertEquals(Tupple2.of("abc", nothing()), State.set("abc").run("zzz"));
  }
  
  @Test
  public void state() {
    assertEquals("ABC", State.<String, String>state(String::toUpperCase).eval("abc"));
  }
  
  @Test
  public void modify() {
    assertEquals(Tupple2.of("ABC", nothing()), State.<String>modify(String::toUpperCase).run("abc"));
  }
  
  @Test
  public void flatMap() {
    State<InmutableList<String>, Nothing> state = 
        unit("a").flatMap(append("b")).flatMap(append("c")).flatMap(end());
    
    Tupple2<InmutableList<String>, Nothing> result = state.run(InmutableList.empty());
    
    assertEquals(Tupple2.of(InmutableList.of("a", "b", "c"), nothing()), result);
  }
  
  @Test
  public void compose() {
    State<Nothing, String> sa = State.unit("a");
    State<Nothing, String> sb = State.unit("b");
    State<Nothing, String> sc = State.unit("c");
    
    Tupple2<Nothing, InmutableList<String>> result = 
        State.compose(InmutableList.of(sa, sb, sc)).run(nothing());
    
    assertEquals(Tupple2.of(nothing(), InmutableList.of("a", "b", "c")), result);
  }

  private static State<InmutableList<String>, String> unit(String value) {
    return State.<InmutableList<String>, String>unit(value);
  }

  private static <T> Handler1<T, State<InmutableList<T>, T>> append(T nextVal) {
    return value -> new State<>(state -> Tupple2.of(state.append(value), nextVal));
  }
  
  private static <T> Handler1<T, State<InmutableList<T>, Nothing>> end() {
    return value -> new State<>(state -> Tupple2.of(state.append(value), nothing()));
  }
}
