/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Nothing.nothing;
import static com.github.tonivade.zeromock.core.State.state;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StateTest {
  
  @Test
  public void get() {
    assertEquals(Tuple2.of("abc", "abc"), State.get().run("abc"));
  }
  
  @Test
  public void set() {
    assertEquals(Tuple2.of("abc", nothing()), State.set("abc").run("zzz"));
  }
  
  @Test
  public void gets() {
    assertEquals("ABC", State.<String, String>gets(String::toUpperCase).eval("abc"));
  }
  
  @Test
  public void modify() {
    assertEquals(Tuple2.of("ABC", nothing()), State.<String>modify(String::toUpperCase).run("abc"));
  }
  
  @Test
  public void flatMap() {
    State<ImmutableList<String>, Nothing> state = 
        unit("a").flatMap(append("b")).flatMap(append("c")).flatMap(end());
    
    Tuple2<ImmutableList<String>, Nothing> result = state.run(ImmutableList.empty());
    
    assertEquals(Tuple2.of(ImmutableList.of("a", "b", "c"), nothing()), result);
  }
  
  @Test
  public void compose() {
    State<Nothing, String> sa = State.unit("a");
    State<Nothing, String> sb = State.unit("b");
    State<Nothing, String> sc = State.unit("c");
    
    Tuple2<Nothing, ImmutableList<String>> result = 
        State.compose(ImmutableList.of(sa, sb, sc)).run(nothing());
    
    assertEquals(Tuple2.of(nothing(), ImmutableList.of("a", "b", "c")), result);
  }

  private static State<ImmutableList<String>, String> unit(String value) {
    return State.<ImmutableList<String>, String>unit(value);
  }

  private static <T> Function1<T, State<ImmutableList<T>, T>> append(T nextVal) {
    return value -> state(state -> Tuple2.of(state.append(value), nextVal));
  }
  
  private static <T> Function1<T, State<ImmutableList<T>, Nothing>> end() {
    return value -> state(state -> Tuple2.of(state.append(value), nothing()));
  }
}
