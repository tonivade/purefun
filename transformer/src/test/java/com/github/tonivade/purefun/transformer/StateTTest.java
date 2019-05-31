/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.transformer.StateT.lift;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.instances.IOInstances;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Monad;

public class StateTTest {

  private Monad<IO.µ> monad = IOInstances.monad();

  @Test
  public void get() {
    Higher1<IO.µ, Tuple2<Object, Object>> run = StateT.get(monad).run("abc");
    assertEquals(Tuple.of("abc", "abc"), IO.narrowK(run).unsafeRunSync());
  }

  @Test
  public void set() {
    Higher1<IO.µ, Tuple2<String, Unit>> run = StateT.set(monad, "abc").run("zzz");
    assertEquals(Tuple.of("abc", unit()), IO.narrowK(run).unsafeRunSync());
  }

  @Test
  public void gets() {
    Higher1<IO.µ, String> eval = StateT.<IO.µ, String, String>inspect(monad, String::toUpperCase).eval("abc");
    assertEquals("ABC", IO.narrowK(eval).unsafeRunSync());
  }

  @Test
  public void modify() {
    Higher1<IO.µ, Tuple2<String, Unit>> run = StateT.<IO.µ, String>modify(monad, String::toUpperCase).run("abc");
    assertEquals(Tuple.of("ABC", unit()), IO.narrowK(run).unsafeRunSync());
  }

  @Test
  public void flatMap() {
    StateT<IO.µ, ImmutableList<String>, Unit> state =
        pure("a").flatMap(append("b")).flatMap(append("c")).flatMap(end());

    IO<Tuple2<ImmutableList<String>, Unit>> result = IO.narrowK(state.run(ImmutableList.empty()));

    assertEquals(Tuple.of(listOf("a", "b", "c"), unit()), result.unsafeRunSync());
  }

  @Test
  public void compose() {
    StateT<IO.µ, Unit, String> sa = StateT.pure(monad, "a");
    StateT<IO.µ, Unit, String> sb = StateT.pure(monad, "b");
    StateT<IO.µ, Unit, String> sc = StateT.pure(monad, "c");

    Higher1<IO.µ, Tuple2<Unit, Sequence<String>>> result = StateT.compose(monad, listOf(sa, sb, sc)).run(unit());

    assertEquals(Tuple.of(unit(), listOf("a", "b", "c")), IO.narrowK(result).unsafeRunSync());
  }

  @Test
  public void run() {
    StateT<IO.µ, ImmutableList<String>, Option<String>> read =
        StateT.lift(monad, state -> Tuple.of(state.tail(), state.head()));

    IO<Tuple2<ImmutableList<String>, Option<String>>> result = IO.narrowK(read.run(listOf("a", "b", "c")));

    assertEquals(Tuple.of(listOf("b", "c"), Option.some("a")), result.unsafeRunSync());
  }

  @Test
  public void mapK() {
    StateT<IO.µ, Unit, String> stateIo = StateT.pure(monad, "abc");

    StateT<Try.µ, Unit, String> stateTry = stateIo.mapK(TryInstances.monad(), new IOToTryTransformer());

    assertEquals(Try.success(Tuple2.of(unit(), "abc")), Try.narrowK(stateTry.run(unit())));
  }

  private StateT<IO.µ, ImmutableList<String>, String> pure(String value) {
    return StateT.pure(monad, value);
  }

  private <T> Function1<T, StateT<IO.µ, ImmutableList<T>, T>> append(T nextVal) {
    return value -> lift(monad, state -> Tuple.of(state.append(value), nextVal));
  }

  private <T> Function1<T, StateT<IO.µ, ImmutableList<T>, Unit>> end() {
    return value -> lift(monad, state -> Tuple.of(state.append(value), unit()));
  }
}
