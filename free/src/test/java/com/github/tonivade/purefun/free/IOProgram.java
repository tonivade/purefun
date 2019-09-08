/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Pattern1;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.StateInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Transformer;

import static com.github.tonivade.purefun.Matcher1.instanceOf;
import static java.util.Objects.requireNonNull;

@HigherKind
public interface IOProgram<T> {

  <R> IOProgram<R> map(Function1<T, R> mapper);

  <R> R fold(Function2<String, T, R> write, Function1<T, R> read);

  final class Read<T> implements IOProgram<T> {

    private final Function1<String, T> next;

    Read(Function1<String, T> next) {
      this.next = requireNonNull(next);
    }

    public Function1<String, T> next() {
      return next;
    }

    @Override
    public <R> IOProgram<R> map(Function1<T, R> mapper) {
      return new Read<>(next.andThen(mapper));
    }

    @Override
    public <R> R fold(Function2<String, T, R> write, Function1<T, R> read) {
      return next.andThen(read).apply("$text");
    }

    @Override
    public String toString() {
      return "Read";
    }
  }

  final class Write<T> implements IOProgram<T> {

    private final String value;
    private final T next;

    Write(String value, T next) {
      this.value = requireNonNull(value);
      this.next = requireNonNull(next);
    }

    public String value() {
      return value;
    }

    public T next() {
      return next;
    }

    @Override
    public <R> IOProgram<R> map(Function1<T, R> mapper) {
      return new Write<>(value, mapper.apply(next));
    }

    @Override
    public <R> R fold(Function2<String, T, R> write, Function1<T, R> read) {
      return write.apply(value, next);
    }

    @Override
    public String toString() {
      return "Write(" + value + ")";
    }
  }

  default Read<T> asRead() {
    return (Read<T>) this;
  }

  default Write<T> asWrite() {
    return (Write<T>) this;
  }
}

class IOProgramToState implements Transformer<IOProgram.µ, Higher1<State.µ, ImmutableList<String>>> {

  private final Console<Higher1<State.µ, ImmutableList<String>>> console = StateInstances.console();

  @Override
  public <X> Higher1<Higher1<State.µ, ImmutableList<String>>, X> apply(Higher1<IOProgram.µ, X> from) {
    return Pattern1.<IOProgram<X>, State<ImmutableList<String>, X>>build()
      .when(instanceOf(IOProgram.Read.class))
      .then(program -> State.narrowK(console.readln()).map(program.asRead().next()))
      .when(instanceOf(IOProgram.Write.class))
      .then(program -> State.narrowK(console.println(program.asWrite().value())).map(ignore -> program.asWrite().next()))
      .apply(IOProgram.narrowK(from)).kind1();
  }
}

class IOProgramToIO implements Transformer<IOProgram.µ, IO.µ> {

  private final Console<IO.µ> console = IOInstances.console();

  @Override
  public <X> Higher1<IO.µ, X> apply(Higher1<IOProgram.µ, X> from) {
    return Pattern1.<IOProgram<X>, IO<X>>build()
      .when(instanceOf(IOProgram.Read.class))
      .then(program -> IO.narrowK(console.readln()).map(program.asRead().next()))
      .when(instanceOf(IOProgram.Write.class))
      .then(program -> IO.narrowK(console.println(program.asWrite().value())).map(ignore -> program.asWrite().next()))
      .apply(IOProgram.narrowK(from)).kind1();
  }
}

class IOProgramFunctor implements Functor<IOProgram.µ> {

  @Override
  public <T, R> Higher1<IOProgram.µ, R> map(Higher1<IOProgram.µ, T> value, Function1<T, R> mapper) {
    return value.fix1(IOProgram::narrowK).map(mapper).kind1();
  }
}
