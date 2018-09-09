package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.monad.Free.liftF;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Transformer;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.monad.IOProgram.µ;

public class FreeTest {

  @Test
  public void echo() {
    Free<IOProgram.µ, Nothing> echo = IOProgram.write("enter your name")
        .flatMap(x -> IOProgram.read())
        .flatMap(s -> IOProgram.write("Hello " + s));

    String show = showProgram(echo);

    System.out.println(show);

//    Higher<IOKind.µ, Nothing> foldMap = echo.foldMap(new IOMonad(), new IOProgramFunctor(), new IOProgramInterperter());
//
//    IOKind.narrowK(foldMap).unsafeRunSync();
  }

  private <R> String showProgram(Free<IOProgram.µ, R> echo) {
    return echo.resume(IOProgram.functor).fold(left -> {
                IOProgram<Free<IOProgram.µ, R>> program = (IOProgram<Free<µ, R>>) left;
                return program.fold((value, next) -> "write(" + value + ") -> (" + showProgram(next) + ")",
                                    (next) -> "read(text) -> " + showProgram(next));
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
      return next.andThen(read).apply("text");
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
}

class IOProgramInterperter implements Transformer<IOProgram.µ, IOKind.µ> {
  @Override
  public <X> IO<X> apply(Higher<IOProgram.µ, X> from) {
    IOProgram<X> program = IOProgram.narrowK(from);
    if (program instanceof IOProgram.Read) {
      IOProgram.Read<X> read = (IOProgram.Read<X>) program;
      return (IO<X>) IO.ConsoleIO.readln();
    }
    if (program instanceof IOProgram.Write) {
      IOProgram.Write<X> write = (IOProgram.Write<X>) program;
      return (IO<X>) IO.ConsoleIO.println(write.value);
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
  public <T, R> IO<R> flatMap(Higher<IOKind.µ, T> value, Function1<T, ? extends Higher<IOKind.µ, R>> map) {
    return IOKind.narrowK(value).flatMap(map);
  }
}

class IOProgramFunctor implements Functor<IOProgram.µ> {

  @Override
  public <T, R> IOProgram<R> map(Higher<IOProgram.µ, T> value, Function1<T, R> map) {
    IOProgram<T> program = IOProgram.narrowK(value);
    if (program instanceof IOProgram.Read) {
      IOProgram.Read<T> read = (IOProgram.Read<T>) program;
      return new IOProgram.Read<>(read.next.andThen(map));
    }
    if (program instanceof IOProgram.Write) {
      IOProgram.Write<T> write = (IOProgram.Write<T>) program;
      return new IOProgram.Write<>(write.value, map.apply(write.next));
    }
    throw new IllegalStateException();
  }
}
