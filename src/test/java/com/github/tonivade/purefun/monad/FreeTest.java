/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.monad.Free.liftF;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.algebra.Functor;
import com.github.tonivade.purefun.algebra.Monad;
import com.github.tonivade.purefun.algebra.Transformer;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.typeclasses.Console;

public class FreeTest {

  final Free<IOProgram.µ, Nothing> echo =
      IOProgram.write("what's your name?")
        .flatMap(ignore -> IOProgram.read())
        .flatMap(text -> IOProgram.write("Hello " + text))
        .flatMap(ignore -> IOProgram.write("end"));

  @Test
  public void showProgram() {
    assertEquals("write(\"what's your name?\") "
                 + "then (text <- read() "
                   + "then (write(\"Hello $text\") "
                     + "then (write(\"end\") "
                       + "then (return(Nothing)))))", showProgram(echo));
  }

  @Test
  public void interpret() {
    Higher<Higher<StateKind.µ, ImmutableList<String>>, Nothing> foldMap = 
        echo.foldMap(new StateMonad(), IOProgram.functor, new IOProgramState());
    
    State<ImmutableList<String>, Nothing> state = StateKind.narrowK(foldMap);
    
    Tuple2<ImmutableList<String>, Nothing> run = state.run(ImmutableList.of("Toni"));

    assertEquals(ImmutableList.of("what's your name?", "Hello Toni", "end"), run.get1());
  }

  private <R> String showProgram(Free<IOProgram.µ, R> program) {
    return program.resume(IOProgram.functor)
        .fold(left -> IOProgram.narrowK(left)
                        .fold((value, next) -> "write(\"" + value + "\") then (" + showProgram(next) + ")",
                              (next) -> "text <- read() then (" + showProgram(next) + ")"),
              right -> "return(" + right + ")");
  }
}

interface IOProgram<T> extends Higher<IOProgram.µ, T> {
  final class µ implements Witness {}

  <R> R fold(Function2<String, T, R> write, Function1<T, R> read);

  Functor<IOProgram.µ> functor = new IOProgramFunctor();

  static Free<IOProgram.µ, String> read() {
    return liftF(functor, new Read<>(identity()));
  }

  static Free<IOProgram.µ, Nothing> write(String value) {
    return liftF(functor, new Write<>(value, nothing()));
  }

  final class Read<T> implements IOProgram<T> {
    final Function1<String, T> next;

    Read(Function1<String, T> next) {
      this.next = next;
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
    final String value;
    final T next;

    Write(String value, T next) {
      this.value = value;
      this.next = next;
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

  static <T> IOProgram<T> narrowK(Higher<IOProgram.µ, T> value) {
    return (IOProgram<T>) value;
  }

  default Read<T> asRead() {
    return (Read<T>) this;
  }

  default Write<T> asWrite() {
    return (Write<T>) this;
  }
}

class IOProgramState implements Transformer<IOProgram.µ, Higher<StateKind.µ, ImmutableList<String>>> {
  private final Console<Higher<StateKind.µ, ImmutableList<String>>> console = Console.state();

  @Override
  public <X> State<ImmutableList<String>, X> apply(Higher<IOProgram.µ, X> from) {
    IOProgram<X> program = IOProgram.narrowK(from);
    if (program instanceof IOProgram.Read) {
      return StateKind.narrowK(console.readln())
          .map(program.asRead().next);
    }
    if (program instanceof IOProgram.Write) {
      return StateKind.narrowK(console.println(program.asWrite().value))
          .map(ignore -> program.asWrite().next);
    }
    throw new IllegalStateException();
  }
}

class StateMonad implements Monad<Higher<StateKind.µ, ImmutableList<String>>> {

  @Override
  public <T> State<ImmutableList<String>, T> pure(T value) {
    return State.pure(value);
  }

  @Override
  public <T, R> State<ImmutableList<String>, R> map(
      Higher<Higher<StateKind.µ, ImmutableList<String>>, T> value, Function1<T, R> map) {
    return StateKind.narrowK(value).map(map);
  }

  @Override
  public <T, R> State<ImmutableList<String>, R> flatMap(
      Higher<Higher<StateKind.µ, ImmutableList<String>>, T> value,
      Function1<T, ? extends Higher<Higher<StateKind.µ, ImmutableList<String>>, R>> map) {
    return StateKind.narrowK(value).flatMap(map.andThen(StateKind::narrowK));
  }
}

class IOProgramFunctor implements Functor<IOProgram.µ> {

  @Override
  public <T, R> IOProgram<R> map(Higher<IOProgram.µ, T> value, Function1<T, R> map) {
    IOProgram<T> program = IOProgram.narrowK(value);
    if (program instanceof IOProgram.Read) {
      return new IOProgram.Read<>(program.asRead().next.andThen(map));
    }
    if (program instanceof IOProgram.Write) {
      return new IOProgram.Write<>(program.asWrite().value,
                                   map.apply(program.asWrite().next));
    }
    throw new IllegalStateException();
  }
}
