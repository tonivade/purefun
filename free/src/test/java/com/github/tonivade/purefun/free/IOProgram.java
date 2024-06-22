/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.free.Free.liftF;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Instances;

public sealed interface IOProgram<T> extends Kind<IOProgram<?>, T> {

  static Free<IOProgram<?>, String> read() {
    return liftF(new IOProgram.Read());
  }

  static Free<IOProgram<?>, Unit> write(String value) {
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
class IOProgramToState implements FunctionK<IOProgram<?>, State<ImmutableList<String>, ?>> {

  private final Console<State<ImmutableList<String>, ?>> console = Instances.console();

  @Override
  public <X> State<ImmutableList<String>, X> apply(Kind<IOProgram<?>, ? extends X> from) {
    return (State<ImmutableList<String>, X>) switch (from.<IOProgram<X>>fix()) {
      case IOProgram.Read() -> console.readln();
      case IOProgram.Write(var value) -> console.println(value);
    };
  }
}

@SuppressWarnings("unchecked")
class IOProgramToIO implements FunctionK<IOProgram<?>, IO<?>> {

  private final Console<IO<?>> console = Instances.console();

  @Override
  public <X> IO<X> apply(Kind<IOProgram<?>, ? extends X> from) {
    return (IO<X>) switch (from.<IOProgram<X>>fix()) {
      case IOProgram.Read() -> console.readln();
      case IOProgram.Write(var value) -> console.println(value);
    };
  }
}
