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
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;
import com.github.tonivade.purefun.typeclasses.Monad;

public class StateTTest {

  private Monad<IO<?>> monad = IOInstances.monad();

  @Test
  public void get() {
    Kind<IO<?>, Tuple2<Object, Object>> run = StateT.get(monad).run("abc");
    assertEquals(Tuple.of("abc", "abc"), run.fix(IOOf::toIO).unsafeRunSync());
  }

  @Test
  public void set() {
    Kind<IO<?>, Tuple2<String, Unit>> run = StateT.set(monad, "abc").run("zzz");
    assertEquals(Tuple.of("abc", unit()), run.fix(IOOf::toIO).unsafeRunSync());
  }

  @Test
  public void gets() {
    Kind<IO<?>, String> eval = StateT.<IO<?>, String, String>inspect(monad, String::toUpperCase).eval("abc");
    assertEquals("ABC", eval.fix(IOOf::toIO).unsafeRunSync());
  }

  @Test
  public void modify() {
    Kind<IO<?>, Tuple2<String, Unit>> run = StateT.<IO<?>, String>modify(monad, String::toUpperCase).run("abc");
    assertEquals(Tuple.of("ABC", unit()), run.fix(IOOf::toIO).unsafeRunSync());
  }

  @Test
  public void flatMap() {
    StateT<IO<?>, ImmutableList<String>, Unit> state =
        pure("a").flatMap(append("b")).flatMap(append("c")).flatMap(end());

    IO<Tuple2<ImmutableList<String>, Unit>> result = IOOf.toIO(state.run(ImmutableList.empty()));

    assertEquals(Tuple.of(listOf("a", "b", "c"), unit()), result.unsafeRunSync());
  }

  @Test
  public void traverse() {
    StateT<IO<?>, Unit, String> sa = StateT.pure(monad, "a");
    StateT<IO<?>, Unit, String> sb = StateT.pure(monad, "b");
    StateT<IO<?>, Unit, String> sc = StateT.pure(monad, "c");

    Kind<IO<?>, Tuple2<Unit, Sequence<String>>> result = StateT.traverse(monad, listOf(sa, sb, sc)).run(unit());

    assertEquals(Tuple.of(unit(), listOf("a", "b", "c")), IOOf.toIO(result).unsafeRunSync());
  }

  @Test
  public void run() {
    StateT<IO<?>, ImmutableList<String>, Option<String>> read =
        StateT.lift(monad, state -> Tuple.of(state.tail(), state.head()));

    IO<Tuple2<ImmutableList<String>, Option<String>>> result = IOOf.toIO(read.run(listOf("a", "b", "c")));

    assertEquals(Tuple.of(listOf("b", "c"), Option.some("a")), result.unsafeRunSync());
  }

  @Test
  public void mapK() {
    StateT<IO<?>, Unit, String> stateIo = StateT.pure(monad, "abc");

    StateT<Try<?>, Unit, String> stateTry = stateIo.mapK(TryInstances.monad(), new IOToTryFunctionK());

    assertEquals(Try.success(Tuple2.of(unit(), "abc")), TryOf.toTry(stateTry.run(unit())));
  }

  private StateT<IO<?>, ImmutableList<String>, String> pure(String value) {
    return StateT.pure(monad, value);
  }

  private <T> Function1<T, StateT<IO<?>, ImmutableList<T>, T>> append(T nextVal) {
    return value -> StateT.lift(monad, state -> Tuple.of(state.append(value), nextVal));
  }

  private <T> Function1<T, StateT<IO<?>, ImmutableList<T>, Unit>> end() {
    return value -> StateT.lift(monad, state -> Tuple.of(state.append(value), unit()));
  }
}
