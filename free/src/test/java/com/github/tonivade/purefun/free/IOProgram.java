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
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.monad.State_;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.FunctionK;

import static com.github.tonivade.purefun.Matcher1.instanceOf;
import static com.github.tonivade.purefun.free.Free.liftF;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

@HigherKind
public interface IOProgram<T> extends Higher1<IOProgram_, T> {

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
class IOProgramToState implements FunctionK<IOProgram_, Higher1<State_, ImmutableList<String>>> {

  private final Console<Higher1<State_, ImmutableList<String>>> console = StateInstances.console();

  @Override
  public <X> Higher1<Higher1<State_, ImmutableList<String>>, X> apply(Higher1<IOProgram_, X> from) {
    return Pattern1.<IOProgram<X>, State<ImmutableList<String>, X>>build()
      .when(instanceOf(IOProgram.Read.class))
        .then(program -> (State<ImmutableList<String>, X>) State_.narrowK(console.readln()))
      .when(instanceOf(IOProgram.Write.class))
        .then(program -> (State<ImmutableList<String>, X>) State_.narrowK(console.println(program.asWrite().value())))
      .apply(IOProgram_.narrowK(from));
  }
}

@SuppressWarnings("unchecked")
class IOProgramToIO implements FunctionK<IOProgram_, IO_> {

  private final Console<IO_> console = IOInstances.console();

  @Override
  public <X> Higher1<IO_, X> apply(Higher1<IOProgram_, X> from) {
    return Pattern1.<IOProgram<X>, IO<X>>build()
      .when(instanceOf(IOProgram.Read.class))
        .then(program -> (IO<X>) console.readln().fix1(IO_::narrowK))
      .when(instanceOf(IOProgram.Write.class))
        .then(program -> (IO<X>) IO_.narrowK(console.println(program.asWrite().value())))
      .apply(IOProgram_.narrowK(from));
  }
}
