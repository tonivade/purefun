/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.typeclasses.Functor;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.free.Free.liftF;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FreeTest {

  Functor<IOProgram.µ> functor = new IOProgramFunctor();

  Free<IOProgram.µ, String> read() {
    return liftF(new IOProgram.Read<>(identity()).kind1());
  }

  Free<IOProgram.µ, Unit> write(String value) {
    return liftF(new IOProgram.Write<>(value, unit()).kind1());
  }

  final Free<IOProgram.µ, Unit> echo =
      write("what's your name?")
        .andThen(read())
        .flatMap(text -> write("Hello " + text))
        .andThen(write("end"));

  @Test
  public void showProgram() {
    assertEquals("write(\"what's your name?\") "
                 + "then (text <- read() "
                   + "then (write(\"Hello $text\") "
                     + "then (write(\"end\") "
                       + "then (return(Unit)))))", showProgram(echo));
  }

  @Test
  public void interpretState() {
    Higher1<Higher1<State.µ, ImmutableList<String>>, Unit> foldMap =
        echo.foldMap(StateInstances.monad(), functor, new IOProgramToState());

    State<ImmutableList<String>, Unit> state = State.narrowK(foldMap);

    Tuple2<ImmutableList<String>, Unit> run = state.run(ImmutableList.of("Toni"));

    assertEquals(ImmutableList.of("what's your name?", "Hello Toni", "end"), run.get1());
  }

  @Test
  public void interpretIO() {
    Higher1<IO.µ, Unit> foldMap =
        echo.foldMap(IOInstances.monad(), functor, new IOProgramToIO());

    IO<Unit> echoIO = IO.narrowK(foldMap);

    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(echoIO);

    assertEquals("what's your name?\nHello Toni\nend\n", executor.getOutput());
  }

  private <R> String showProgram(Free<IOProgram.µ, R> program) {
    return program.resume(functor)
        .fold(left -> IOProgram.narrowK(left)
                        .fold((value, next) -> "write(\"" + value + "\") then (" + showProgram(next) + ")",
                              (next) -> "text <- read() then (" + showProgram(next) + ")"),
              right -> "return(" + right + ")");
  }
}
