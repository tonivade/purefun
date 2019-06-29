/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.StateInstances;
import com.github.tonivade.purefun.typeclasses.For;
import com.github.tonivade.purefun.typeclasses.Monad;

public class TaglessTest {

  final Program<Higher1<State.µ, ImmutableList<String>>> stateProgram =
      new Program<>(StateInstances.monad(), new StateProgramInterpreter());
  final Program<IO.µ> ioProgram =
      new Program<>(IOInstances.monad(), new IOProgramInterpreter());

  @Test
  public void stateInterpreter() {
    State<ImmutableList<String>, Unit> state = stateProgram.echo().fix1(State::narrowK);

    Tuple2<ImmutableList<String>, Unit> run = state.run(listOf("Toni"));

    assertEquals(listOf("what's your name?", "Hello Toni"), run.get1());
  }

  @Test
  public void ioInterpreter() {
    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(ioProgram.echo().fix1(IO::narrowK));

    assertEquals("what's your name?\nHello Toni\n", executor.getOutput());
  }
}

interface ProgramK<F extends Kind> {

  Higher1<F, String> read();

  Higher1<F, Unit> write(String string);
}

class Program<F extends Kind> {

  final Monad<F> monad;
  final ProgramK<F> io;

  Program(Monad<F> monad, ProgramK<F> io) {
    this.monad = requireNonNull(monad);
    this.io = requireNonNull(io);
  }

  Higher1<F, Unit> echo() {
    return For.with(monad)
        .andThen(this::whatsYourName)
        .andThen(this::readName)
        .flatMap(this::sayHello)
        .get();
  }

  private Higher1<F, Unit> whatsYourName() {
    return io.write("what's your name?");
  }

  private Higher1<F, String> readName() {
    return io.read();
  }

  private Higher1<F, Unit> sayHello(String name) {
    return io.write("Hello " + name);
  }
}

class IOProgramInterpreter implements ProgramK<IO.µ> {

  final Console<IO.µ> console = Console.io();

  @Override
  public IO<String> read() {
    return console.readln().fix1(IO::narrowK);
  }

  @Override
  public IO<Unit> write(String string) {
    return console.println(string).fix1(IO::narrowK);
  }
}

class StateProgramInterpreter
    implements ProgramK<Higher1<State.µ, ImmutableList<String>>> {

  final Console<Higher1<State.µ, ImmutableList<String>>> console = Console.state();

  @Override
  public State<ImmutableList<String>, String> read() {
    return console.readln().fix1(State::narrowK);
  }

  @Override
  public State<ImmutableList<String>, Unit> write(String string) {
    return console.println(string).fix1(State::narrowK);
  }
}