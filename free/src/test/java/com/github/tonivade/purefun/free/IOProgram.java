/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Pattern1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.StateInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.FunctionK;

import static com.github.tonivade.purefun.Matcher1.instanceOf;
import static com.github.tonivade.purefun.free.Free.liftF;
import static java.util.Objects.requireNonNull;

@HigherKind
public interface IOProgram<T> {

  static Free<IOProgram.µ, String> read() {
    return liftF(new IOProgram.Read().kind1());
  }

  static Free<IOProgram.µ, Unit> write(String value) {
    return liftF(new IOProgram.Write(value).kind1());
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
      this.value = requireNonNull(value);
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

class IOProgramToState implements FunctionK<IOProgram.µ, Higher1<State.µ, ImmutableList<String>>> {

  private final Console<Higher1<State.µ, ImmutableList<String>>> console = StateInstances.console();

  @Override
  public <X> Higher1<Higher1<State.µ, ImmutableList<String>>, X> apply(Higher1<IOProgram.µ, X> from) {
    return Pattern1.<IOProgram<X>, State<ImmutableList<String>, X>>build()
      .when(instanceOf(IOProgram.Read.class))
        .then(program -> (State<ImmutableList<String>, X>) State.narrowK(console.readln()))
      .when(instanceOf(IOProgram.Write.class))
        .then(program -> (State<ImmutableList<String>, X>) State.narrowK(console.println(program.asWrite().value())))
      .apply(IOProgram.narrowK(from)).kind1();
  }
}

class IOProgramToIO implements FunctionK<IOProgram.µ, IO.µ> {

  private final Console<IO.µ> console = IOInstances.console();

  @Override
  public <X> Higher1<IO.µ, X> apply(Higher1<IOProgram.µ, X> from) {
    return Pattern1.<IOProgram<X>, IO<X>>build()
      .when(instanceOf(IOProgram.Read.class))
        .then(program -> (IO<X>) console.readln().fix1(IO::narrowK))
      .when(instanceOf(IOProgram.Write.class))
        .then(program -> (IO<X>) IO.narrowK(console.println(program.asWrite().value())))
      .apply(IOProgram.narrowK(from)).kind1();
  }
}
