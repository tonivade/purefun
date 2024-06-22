/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.typeclasses.Instances;
import org.junit.jupiter.api.Test;

public class FreeTest {

  private final Free<IOProgram<?>, Unit> echo =
      IOProgram.write("what's your name?")
        .andThen(IOProgram.read())
        .flatMap(text -> IOProgram.write("Hello " + text))
        .andThen(IOProgram.write("end"));

  @Test
  public void interpretState() {
    var foldMap = echo.foldMap(Instances.monad(), new IOProgramToState());

    State<ImmutableList<String>, Unit> state = foldMap.fix();

    Tuple2<ImmutableList<String>, Unit> run = state.run(ImmutableList.of("Toni"));

    assertEquals(ImmutableList.of("what's your name?", "Hello Toni", "end"), run.get1());
  }

  @Test
  public void interpretIO() {
    var foldMap = echo.foldMap(Instances.monad(), new IOProgramToIO());

    var executor = new ConsoleExecutor().read("Toni");

    IO<Unit> fix = foldMap.fix();
    executor.run(fix);

    assertEquals("what's your name?\nHello Toni\nend\n", executor.getOutput());
  }
}
