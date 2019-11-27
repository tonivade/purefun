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
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.For;
import com.github.tonivade.purefun.typeclasses.Monad;

public class TaglessTest {

  private final Program<Higher1<State.µ, ImmutableList<String>>> stateProgram =
      new Program<>(StateInstances.monad(), StateInstances.console());
  private final Program<IO.µ> ioProgram =
      new Program<>(IOInstances.monad(), IOInstances.console());

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

class Program<F extends Kind> {

  private final Monad<F> monad;
  private final Console<F> console;

  Program(Monad<F> monad, Console<F> console) {
    this.monad = requireNonNull(monad);
    this.console = requireNonNull(console);
  }

  public Higher1<F, Unit> echo() {
    return For.with(monad)
        .andThen(this::whatsYourName)
        .andThen(this::readName)
        .flatMap(this::sayHello)
        .run();
  }

  private Higher1<F, Unit> whatsYourName() {
    return console.println("what's your name?");
  }

  private Higher1<F, String> readName() {
    return console.readln();
  }

  private Higher1<F, Unit> sayHello(String name) {
    return console.println("Hello " + name);
  }
}
