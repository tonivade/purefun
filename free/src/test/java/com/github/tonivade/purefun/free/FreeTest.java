/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.StateInstances;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.monad.StateOf;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.typeclasses.Instances;

public class FreeTest {

  private final Free<IOProgram_, Unit> echo =
      IOProgram.write("what's your name?")
        .andThen(IOProgram.read())
        .flatMap(text -> IOProgram.write("Hello " + text))
        .andThen(IOProgram.write("end"));

  @Test
  public void interpretState() {
    var foldMap = echo.foldMap(StateInstances.monad(), new IOProgramToState());

    State<ImmutableList<String>, Unit> state = StateOf.narrowK(foldMap);

    Tuple2<ImmutableList<String>, Unit> run = state.run(ImmutableList.of("Toni"));

    assertEquals(ImmutableList.of("what's your name?", "Hello Toni", "end"), run.get1());
  }

  @Test
  public void interpretIO() {
    var foldMap = echo.foldMap(Instances.<IO_>monad(), new IOProgramToIO());

    var executor = new ConsoleExecutor().read("Toni");

    executor.run(foldMap.fix(toIO()));

    assertEquals("what's your name?\nHello Toni\nend\n", executor.getOutput());
  }
}
