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
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.algebra.Functor;
import com.github.tonivade.purefun.algebra.Monad;
import com.github.tonivade.purefun.algebra.Transformer;

public class FreeTest {

  final Free<IOProgram.µ, Nothing> echo =
      IOProgram.write("what's your name?")
        .flatMap(ignore -> IOProgram.read())
        .flatMap(text -> IOProgram.write("Hello " + text))
        .flatMap(ignore -> IOProgram.write("end"));

  @Test
  public void showProgram() {
    assertEquals("write(\"what's your name?\") then "
               + "(text <- read() "
               + "then write(\"Hello $text\") "
               + "then (write(\"end\") "
               + "then (return(Nothing))))", showProgram(echo));
  }

  @Test
  public void interpret() {
    Higher<IOKind.µ, Nothing> foldMap = echo.foldMap(new IOMonad(),
                                                     new IOProgramFunctor(),
                                                     new IOProgramInterperter());

    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(IOKind.narrowK(foldMap));

    assertEquals("what's your name?\nHello Toni\nend\n", executor.getOutput());
  }

  private <R> String showProgram(Free<IOProgram.µ, R> echo) {
    return echo.resume(IOProgram.functor)
        .fold(left -> {
                IOProgram<Free<IOProgram.µ, R>> program = (IOProgram<Free<IOProgram.µ, R>>) left;
                return program.fold((value, next) -> "write(\"" + value + "\") then (" + showProgram(next) + ")",
                                    (next) -> "text <- read() then " + showProgram(next));
              },
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

class IOProgramInterperter implements Transformer<IOProgram.µ, IOKind.µ> {
  @Override
  public <X> IO<X> apply(Higher<IOProgram.µ, X> from) {
    IOProgram<X> program = IOProgram.narrowK(from);
    if (program instanceof IOProgram.Read) {
      return IO.ConsoleIO.readln().map(program.asRead().next);
    }
    if (program instanceof IOProgram.Write) {
      return IO.ConsoleIO.println(program.asWrite().value).map(x -> program.asWrite().next);
    }
    throw new IllegalStateException();
  }
}

class IOMonad implements Monad<IOKind.µ> {

  @Override
  public <T> IO<T> pure(T value) {
    return IO.unit(value);
  }

  @Override
  public <T, R> IO<R> map(Higher<IOKind.µ, T> value, Function1<T, R> map) {
    return IOKind.narrowK(value).map(map);
  }

  @Override
  public <T, R> IO<R> flatMap(Higher<IOKind.µ, T> value,
                              Function1<T, ? extends Higher<IOKind.µ, R>> map) {
    return IOKind.narrowK(value).flatMap(map);
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
