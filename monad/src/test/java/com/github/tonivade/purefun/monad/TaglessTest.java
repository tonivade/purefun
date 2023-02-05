/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static com.github.tonivade.purefun.monad.StateOf.toState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
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

  private final Program<Kind<State_, ImmutableList<String>>> stateProgram =
      new Program<>(StateInstances.monad(), StateInstances.console());
  private final Program<IO_> ioProgram =
      new Program<>(IOInstances.monad(), IOInstances.console());

  @Test
  public void stateInterpreter() {
    State<ImmutableList<String>, Unit> state = stateProgram.echo().fix(toState());

    Tuple2<ImmutableList<String>, Unit> run = state.run(listOf("Toni"));

    assertEquals(listOf("what's your name?", "Hello Toni"), run.get1());
  }

  @Test
  public void ioInterpreter() {
    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(ioProgram.echo().fix(toIO()));

    assertEquals("what's your name?\nHello Toni\n", executor.getOutput());
  }
}

class Program<F extends Witness> {

  private final Monad<F> monad;
  private final Console<F> console;

  Program(Monad<F> monad, Console<F> console) {
    this.monad = checkNonNull(monad);
    this.console = checkNonNull(console);
  }

  public Kind<F, Unit> echo() {
    return For.with(monad)
        .andThen(this::whatsYourName)
        .andThen(this::readName)
        .flatMap(this::sayHello)
        .run();
  }

  private Kind<F, Unit> whatsYourName() {
    return console.println("what's your name?");
  }

  private Kind<F, String> readName() {
    return console.readln();
  }

  private Kind<F, Unit> sayHello(String name) {
    return console.println("Hello " + name);
  }
}
