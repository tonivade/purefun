/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.StateInstances;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.monad.StateOf;
import com.github.tonivade.purefun.monad.State_;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;

public class FreeTest {

  private final Free<IOProgram_, Unit> echo =
      IOProgram.write("what's your name?")
        .andThen(IOProgram.read())
        .flatMap(text -> IOProgram.write("Hello " + text))
        .andThen(IOProgram.write("end"));

  @Test
  public void interpretState() {
    Kind<Kind<State_, ImmutableList<String>>, Unit> foldMap =
        echo.foldMap(StateInstances.monad(), new IOProgramToState());

    State<ImmutableList<String>, Unit> state = StateOf.narrowK(foldMap);

    Tuple2<ImmutableList<String>, Unit> run = state.run(ImmutableList.of("Toni"));

    assertEquals(ImmutableList.of("what's your name?", "Hello Toni", "end"), run.get1());
  }

  @Test
  public void interpretIO() {
    Kind<IO_, Unit> foldMap =
        echo.foldMap(IOInstances.monad(), new IOProgramToIO());

    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(foldMap.fix(toIO()));

    assertEquals("what's your name?\nHello Toni\nend\n", executor.getOutput());
  }
}
