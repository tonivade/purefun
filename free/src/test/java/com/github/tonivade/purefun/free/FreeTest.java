/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.StateInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.monad.State_;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FreeTest {

  private final Free<IOProgram_, Unit> echo =
      IOProgram.write("what's your name?")
        .andThen(IOProgram.read())
        .flatMap(text -> IOProgram.write("Hello " + text))
        .andThen(IOProgram.write("end"));

  @Test
  public void interpretState() {
    Higher1<Higher1<State_, ImmutableList<String>>, Unit> foldMap =
        echo.foldMap(StateInstances.monad(), new IOProgramToState());

    State<ImmutableList<String>, Unit> state = State_.narrowK(foldMap);

    Tuple2<ImmutableList<String>, Unit> run = state.run(ImmutableList.of("Toni"));

    assertEquals(ImmutableList.of("what's your name?", "Hello Toni", "end"), run.get1());
  }

  @Test
  public void interpretIO() {
    Higher1<IO_, Unit> foldMap =
        echo.foldMap(IOInstances.monad(), new IOProgramToIO());

    IO<Unit> echoIO = IO_.narrowK(foldMap);

    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(echoIO);

    assertEquals("what's your name?\nHello Toni\nend\n", executor.getOutput());
  }
}
