/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.monad.State.state;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Option;

public class StateTest {

  @Test
  public void get() {
    assertEquals(Tuple.of("abc", "abc"), State.get().run("abc"));
  }

  @Test
  public void set() {
    assertEquals(Tuple.of("abc", nothing()), State.set("abc").run("zzz"));
  }

  @Test
  public void gets() {
    assertEquals("ABC", State.<String, String>inspect(String::toUpperCase).eval("abc"));
  }

  @Test
  public void modify() {
    assertEquals(Tuple.of("ABC", nothing()), State.<String>modify(String::toUpperCase).run("abc"));
  }

  @Test
  public void flatMap() {
    State<ImmutableList<String>, Nothing> state =
        pure("a").flatMap(append("b")).flatMap(append("c")).flatMap(end());

    Tuple2<ImmutableList<String>, Nothing> result = state.run(ImmutableList.empty());

    assertEquals(Tuple.of(listOf("a", "b", "c"), nothing()), result);
  }

  @Test
  public void compose() {
    State<Nothing, String> sa = State.pure("a");
    State<Nothing, String> sb = State.pure("b");
    State<Nothing, String> sc = State.pure("c");

    Tuple2<Nothing, Sequence<String>> result = State.compose(listOf(sa, sb, sc)).run(nothing());

    assertEquals(Tuple.of(nothing(), listOf("a", "b", "c")), result);
  }

  @Test
  public void run() {
    State<ImmutableList<String>, Option<String>> read = state(state -> Tuple.of(state.tail(), state.head()));

    Tuple2<ImmutableList<String>, Option<String>> result = read.run(listOf("a", "b", "c"));

    assertEquals(Tuple.of(listOf("b", "c"), Option.some("a")), result);
  }

  private static State<ImmutableList<String>, String> pure(String value) {
    return State.pure(value);
  }

  private static <T> Function1<T, State<ImmutableList<T>, T>> append(T nextVal) {
    return value -> state(state -> Tuple.of(state.append(value), nextVal));
  }

  private static <T> Function1<T, State<ImmutableList<T>, Nothing>> end() {
    return value -> state(state -> Tuple.of(state.append(value), nothing()));
  }
}
