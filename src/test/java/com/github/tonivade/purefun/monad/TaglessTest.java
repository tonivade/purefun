/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Monad;

public class TaglessTest {

  final Program<Higher1<State.µ, ImmutableList<String>>, StateProgramInterpreter> stateProgram =
      new Program<>(new StateProgramInterpreter());
  final Program<IO.µ, IOProgramInterpreter> ioProgram = new Program<>(new IOProgramInterpreter());

  @Test
  public void stateInterpreter() {
    State<ImmutableList<String>, Nothing> state = State.narrowK(stateProgram.echo());

    Tuple2<ImmutableList<String>, Nothing> run = state.run(listOf("Toni"));

    assertEquals(listOf("what's your name?", "Hello Toni"), run.get1());
  }

  @Test
  public void ioInterpreter() {
    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(IO.narrowK(ioProgram.echo()));

    assertEquals("what's your name?\nHello Toni\n", executor.getOutput());
  }
}

interface IOProgramT<F extends Kind> {

  Higher1<F, String> read();

  Higher1<F, Nothing> write(String string);
}

class Program<F extends Kind, P extends IOProgramT<F> & Monad<F>> {

  final P program;

  Program(P program) {
    this.program = requireNonNull(program);
  }

  Higher1<F, Nothing> echo() {
    return program.flatMap(readName(), this::sayHello);
  }

  private Higher1<F, String> readName() {
    return program.flatMap(program.write("what's your name?"), nothing -> program.read());
  }

  private Higher1<F, Nothing> sayHello(String name) {
    return program.write("Hello " + name);
  }
}

class IOProgramInterpreter implements IOProgramT<IO.µ>, Monad<IO.µ> {

  final Monad<IO.µ> monad = IO.monad();
  final Console<IO.µ> console = Console.io();

  @Override
  public <T> IO<T> pure(T value) {
    return IO.narrowK(monad.pure(value));
  }

  @Override
  public <T, R> IO<R> flatMap(Higher1<IO.µ, T> value, Function1<T, ? extends Higher1<IO.µ, R>> map) {
    return IO.narrowK(monad.flatMap(value, map));
  }

  @Override
  public IO<String> read() {
    return IO.narrowK(console.readln());
  }

  @Override
  public IO<Nothing> write(String string) {
    return IO.narrowK(console.println(string));
  }
}

class StateProgramInterpreter
    implements IOProgramT<Higher1<State.µ, ImmutableList<String>>>,
               Monad<Higher1<State.µ, ImmutableList<String>>> {

  final Monad<Higher1<State.µ, ImmutableList<String>>> monad = State.monad();
  final Console<Higher1<State.µ, ImmutableList<String>>> console = Console.state();

  @Override
  public <T> State<ImmutableList<String>, T> pure(T value) {
    return State.narrowK(monad.pure(value));
  }

  @Override
  public <T, R> State<ImmutableList<String>, R> flatMap(
      Higher1<Higher1<State.µ, ImmutableList<String>>, T> value,
      Function1<T, ? extends Higher1<Higher1<State.µ, ImmutableList<String>>, R>> map) {
    return State.narrowK(monad.flatMap(value, map));
  }

  @Override
  public State<ImmutableList<String>, String> read() {
    return State.narrowK(console.readln());
  }

  @Override
  public State<ImmutableList<String>, Nothing> write(String string) {
    return State.narrowK(console.println(string));
  }
}