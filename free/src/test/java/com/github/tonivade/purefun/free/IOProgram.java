/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.free.Free.liftF;
import static com.github.tonivade.purefun.free.IOProgramOf.toIOProgram;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.StateInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.monad.State_;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.FunctionK;

@HigherKind
public sealed interface IOProgram<T> extends IOProgramOf<T> {

  static Free<IOProgram_, String> read() {
    return liftF(new IOProgram.Read());
  }

  static Free<IOProgram_, Unit> write(String value) {
    return liftF(new IOProgram.Write(value));
  }

  record Read() implements IOProgram<String> {

  }

  record Write(String value) implements IOProgram<Unit> {

    public Write {
      checkNonNull(value);
    }
  }
}

@SuppressWarnings("unchecked")
class IOProgramToState implements FunctionK<IOProgram_, Kind<State_, ImmutableList<String>>> {

  private final Console<Kind<State_, ImmutableList<String>>> console = StateInstances.console();

  @Override
  public <X> Kind<Kind<State_, ImmutableList<String>>, X> apply(Kind<IOProgram_, ? extends X> from) {
    return switch (from.fix(toIOProgram())) {
      case IOProgram.Read() -> (State<ImmutableList<String>, X>) console.readln();
      case IOProgram.Write(var value) -> (State<ImmutableList<String>, X>) console.println(value);
    };
  }
}

@SuppressWarnings("unchecked")
class IOProgramToIO implements FunctionK<IOProgram_, IO_> {

  private final Console<IO_> console = IOInstances.console();

  @Override
  public <X> Kind<IO_, X> apply(Kind<IOProgram_, ? extends X> from) {
    return switch (from.fix(toIOProgram())) {
      case IOProgram.Read() -> (IO<X>) console.readln();
      case IOProgram.Write(var value) -> (IO<X>) console.println(value);
    };
  }
}
