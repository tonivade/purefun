/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.core.Unit.unit;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;
import com.github.tonivade.purefun.type.Try_;
import com.github.tonivade.purefun.typeclasses.Monad;

public class StateTTest {

  private Monad<IO_> monad = IOInstances.monad();

  @Test
  public void get() {
    Kind<IO_, Tuple2<Object, Object>> run = StateT.get(monad).run("abc");
    assertEquals(Tuple.of("abc", "abc"), run.fix(toIO()).unsafeRunSync());
  }

  @Test
  public void set() {
    Kind<IO_, Tuple2<String, Unit>> run = StateT.set(monad, "abc").run("zzz");
    assertEquals(Tuple.of("abc", unit()), run.fix(toIO()).unsafeRunSync());
  }

  @Test
  public void gets() {
    Kind<IO_, String> eval = StateT.<IO_, String, String>inspect(monad, String::toUpperCase).eval("abc");
    assertEquals("ABC", eval.fix(toIO()).unsafeRunSync());
  }

  @Test
  public void modify() {
    Kind<IO_, Tuple2<String, Unit>> run = StateT.<IO_, String>modify(monad, String::toUpperCase).run("abc");
    assertEquals(Tuple.of("ABC", unit()), run.fix(toIO()).unsafeRunSync());
  }

  @Test
  public void flatMap() {
    StateT<IO_, ImmutableList<String>, Unit> state =
        pure("a").flatMap(append("b")).flatMap(append("c")).flatMap(end());

    IO<Tuple2<ImmutableList<String>, Unit>> result = IOOf.narrowK(state.run(ImmutableList.empty()));

    assertEquals(Tuple.of(listOf("a", "b", "c"), unit()), result.unsafeRunSync());
  }

  @Test
  public void traverse() {
    StateT<IO_, Unit, String> sa = StateT.pure(monad, "a");
    StateT<IO_, Unit, String> sb = StateT.pure(monad, "b");
    StateT<IO_, Unit, String> sc = StateT.pure(monad, "c");

    Kind<IO_, Tuple2<Unit, Sequence<String>>> result = StateT.traverse(monad, listOf(sa, sb, sc)).run(unit());

    assertEquals(Tuple.of(unit(), listOf("a", "b", "c")), IOOf.narrowK(result).unsafeRunSync());
  }

  @Test
  public void run() {
    StateT<IO_, ImmutableList<String>, Option<String>> read =
        StateT.lift(monad, state -> Tuple.of(state.tail(), state.head()));

    IO<Tuple2<ImmutableList<String>, Option<String>>> result = IOOf.narrowK(read.run(listOf("a", "b", "c")));

    assertEquals(Tuple.of(listOf("b", "c"), Option.some("a")), result.unsafeRunSync());
  }

  @Test
  public void mapK() {
    StateT<IO_, Unit, String> stateIo = StateT.pure(monad, "abc");

    StateT<Try_, Unit, String> stateTry = stateIo.mapK(TryInstances.monad(), new IOToTryFunctionK());

    assertEquals(Try.success(Tuple2.of(unit(), "abc")), TryOf.narrowK(stateTry.run(unit())));
  }

  private StateT<IO_, ImmutableList<String>, String> pure(String value) {
    return StateT.pure(monad, value);
  }

  private <T> Function1<T, StateT<IO_, ImmutableList<T>, T>> append(T nextVal) {
    return value -> StateT.lift(monad, state -> Tuple.of(state.append(value), nextVal));
  }

  private <T> Function1<T, StateT<IO_, ImmutableList<T>, Unit>> end() {
    return value -> StateT.lift(monad, state -> Tuple.of(state.append(value), unit()));
  }
}
