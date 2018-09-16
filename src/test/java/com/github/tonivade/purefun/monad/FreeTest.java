/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Matcher.instanceOf;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.monad.Free.liftF;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Pattern;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher;
import com.github.tonivade.purefun.algebra.Functor;
import com.github.tonivade.purefun.algebra.Transformer;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.typeclasses.Console;

public class FreeTest {

  final Free<IOProgram.µ, Nothing> echo =
      IOProgram.write("what's your name?")
        .andThen(IOProgram.read())
        .flatMap(text -> IOProgram.write("Hello " + text))
        .andThen(IOProgram.write("end"));

  @Test
  public void showProgram() {
    assertEquals("write(\"what's your name?\") "
                 + "then (text <- read() "
                   + "then (write(\"Hello $text\") "
                     + "then (write(\"end\") "
                       + "then (return(Nothing)))))", showProgram(echo));
  }

  @Test
  public void interpretState() {
    Higher1<Higher1<State.µ, ImmutableList<String>>, Nothing> foldMap =
        echo.foldMap(State.monad(), IOProgram.functor, new IOProgramToState());

    State<ImmutableList<String>, Nothing> state = State.narrowK(foldMap);

    Tuple2<ImmutableList<String>, Nothing> run = state.run(ImmutableList.of("Toni"));

    assertEquals(ImmutableList.of("what's your name?", "Hello Toni", "end"), run.get1());
  }

  @Test
  public void interpretIO() {
    Higher1<IO.µ, Nothing> foldMap =
        echo.foldMap(IO.monad(), IOProgram.functor, new IOProgramToIO());

    IO<Nothing> echoIO = IO.narrowK(foldMap);

    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(echoIO);

    assertEquals("what's your name?\nHello Toni\nend\n", executor.getOutput());
  }

  private <R> String showProgram(Free<IOProgram.µ, R> program) {
    return program.resume(IOProgram.functor)
        .fold(left -> IOProgram.narrowK(left)
                        .fold((value, next) -> "write(\"" + value + "\") then (" + showProgram(next) + ")",
                              (next) -> "text <- read() then (" + showProgram(next) + ")"),
              right -> "return(" + right + ")");
  }
}

interface IOProgram<T> extends Higher1<IOProgram.µ, T> {
  final class µ implements Kind {}

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

  static <T> IOProgram<T> narrowK(Higher1<IOProgram.µ, T> value) {
    return (IOProgram<T>) value;
  }

  default Read<T> asRead() {
    return (Read<T>) this;
  }

  default Write<T> asWrite() {
    return (Write<T>) this;
  }
}

class IOProgramToState implements Transformer<IOProgram.µ, Higher1<State.µ, ImmutableList<String>>> {

  private final Console<Higher1<State.µ, ImmutableList<String>>> console = Console.state();

  @Override
  public <X> State<ImmutableList<String>, X> apply(Higher1<IOProgram.µ, X> from) {
    Matcher<IOProgram<X>> isRead = Matcher.instanceOf(IOProgram.Read.class);
    Matcher<IOProgram<X>> isWriter = Matcher.instanceOf(IOProgram.Write.class);
    return Pattern.<IOProgram<X>, State<ImmutableList<String>, X>>build()
      .when(isRead)
        .then(program -> State.narrowK(console.readln()).map(program.asRead().next))
      .when(isWriter)
        .then(program -> State.narrowK(console.println(program.asWrite().value)).map(ignore -> program.asWrite().next))
      .apply(IOProgram.narrowK(from));
  }
}

class IOProgramToIO implements Transformer<IOProgram.µ, IO.µ> {

  private final Console<IO.µ> console = Console.io();

  @Override
  public <X> IO<X> apply(Higher1<IOProgram.µ, X> from) {
    return Pattern.<IOProgram<X>, IO<X>>build()
      .when(instanceOf(IOProgram.Read.class))
        .then(program -> IO.narrowK(console.readln()).map(program.asRead().next))
      .when(instanceOf(IOProgram.Write.class))
        .then(program -> IO.narrowK(console.println(program.asWrite().value)).map(ignore -> program.asWrite().next))
      .apply(IOProgram.narrowK(from));
  }
}

class IOProgramFunctor implements Functor<IOProgram.µ> {

  @Override
  public <T, R> IOProgram<R> map(Higher1<IOProgram.µ, T> value, Function1<T, R> map) {
    return Pattern.<IOProgram<T>, IOProgram<R>>build()
      .when(instanceOf(IOProgram.Read.class))
        .then(program -> new IOProgram.Read<>(program.asRead().next.andThen(map)))
      .when(instanceOf(IOProgram.Write.class))
        .then(program -> new IOProgram.Write<>(program.asWrite().value, map.apply(program.asWrite().next)))
      .apply(IOProgram.narrowK(value));
  }
}
