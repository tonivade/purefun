package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.monad.Free.liftF;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.monad.IOKind.µ;

public class FreeTest {

  @Test
  public void echo() {
    Free<IOProgram.µ, Nothing> echo = IOProgram.read().flatMap(IOProgram::write);

    Higher<IOKind.µ, Nothing> foldMap = echo.foldMap(new IOMonad(), new IOProgramInterperter());

    IOKind.narrowK(foldMap).unsafeRunSync();
  }
}

interface IOProgram<T> extends Higher<IOProgram.µ, T> {
  final class µ implements Witness {}

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
  }

  final class Write<T> implements IOProgram<T> {
    final String value;
    final T next;

    Write(String value, T next) {
      this.next = next;
      this.value = value;
    }
  }

  static <T> IOProgram<T> narrowK(Higher<IOProgram.µ, T> value) {
    return (IOProgram<T>) value;
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
    return null;
  }
}

class IOMonad implements Monad<IOKind.µ> {

  @Override
  public <T> IO<T> pure(T value) {
    return IO.unit(value);
  }

  @Override
  public <T, R> IO<R> map(Higher<µ, T> value, Function1<T, R> map) {
    return IOKind.narrowK(value).map(map);
  }

  @Override
  public <T, R> IO<R> flatMap(Higher<µ, T> value, Function1<T, ? extends Higher<µ, R>> map) {
    return IOKind.narrowK(value).flatMap(map);
  }
}