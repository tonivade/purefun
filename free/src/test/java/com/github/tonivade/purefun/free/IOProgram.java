/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Matcher1.instanceOf;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.free.Free.liftF;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Pattern1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.StateInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.monad.StateOf;
import com.github.tonivade.purefun.monad.State_;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.FunctionK;

@HigherKind
public interface IOProgram<T> extends IOProgramOf<T> {

  static Free<IOProgram_, String> read() {
    return liftF(new IOProgram.Read());
  }

  static Free<IOProgram_, Unit> write(String value) {
    return liftF(new IOProgram.Write(value));
  }

  final class Read implements IOProgram<String> {

    private Read() { }

    @Override
    public String toString() {
      return "Read";
    }
  }

  final class Write implements IOProgram<Unit> {

    private final String value;

    private Write(String value) {
      this.value = checkNonNull(value);
    }

    public String value() {
      return value;
    }

    @Override
    public String toString() {
      return "Write(" + value + ")";
    }
  }

  default Read asRead() {
    return (Read) this;
  }

  default Write asWrite() {
    return (Write) this;
  }
}

@SuppressWarnings("unchecked")
class IOProgramToState implements FunctionK<IOProgram_, Kind<State_, ImmutableList<String>>> {

  private final Console<Kind<State_, ImmutableList<String>>> console = StateInstances.console();

  @Override
  public <X> Kind<Kind<State_, ImmutableList<String>>, X> apply(Kind<IOProgram_, X> from) {
    return Pattern1.<IOProgram<X>, State<ImmutableList<String>, X>>build()
      .when(instanceOf(IOProgram.Read.class))
        .then(program -> (State<ImmutableList<String>, X>) StateOf.narrowK(console.readln()))
      .when(instanceOf(IOProgram.Write.class))
        .then(program -> (State<ImmutableList<String>, X>) StateOf.narrowK(console.println(program.asWrite().value())))
      .apply(IOProgramOf.narrowK(from));
  }
}

@SuppressWarnings("unchecked")
class IOProgramToIO implements FunctionK<IOProgram_, IO_> {

  private final Console<IO_> console = IOInstances.console();

  @Override
  public <X> Kind<IO_, X> apply(Kind<IOProgram_, X> from) {
    return Pattern1.<IOProgram<X>, IO<X>>build()
      .when(instanceOf(IOProgram.Read.class))
        .then(program -> (IO<X>) console.readln().fix(IOOf::narrowK))
      .when(instanceOf(IOProgram.Write.class))
        .then(program -> (IO<X>) IOOf.narrowK(console.println(program.asWrite().value())))
      .apply(IOProgramOf.narrowK(from));
  }
}
