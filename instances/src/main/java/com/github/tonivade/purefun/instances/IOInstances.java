/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.monad.IOOf.toIO;
import java.time.Duration;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Timer;

public interface IOInstances {

  static Functor<IO_> functor() {
    return IOFunctor.INSTANCE;
  }

  static Monad<IO_> monad() {
    return IOMonad.INSTANCE;
  }

  static MonadError<IO_, Throwable> monadError() {
    return IOMonadError.INSTANCE;
  }

  static MonadThrow<IO_> monadThrow() {
    return IOMonadThrow.INSTANCE;
  }

  static Timer<IO_> timer() {
    return IOMonadDefer.INSTANCE;
  }

  static MonadDefer<IO_> monadDefer() {
    return IOMonadDefer.INSTANCE;
  }

  static Console<IO_> console() {
    return ConsoleIO.INSTANCE;
  }
}

interface IOFunctor extends Functor<IO_> {

  IOFunctor INSTANCE = new IOFunctor() {};

  @Override
  default <T, R> Kind<IO_, R> map(Kind<IO_, T> value, Function1<T, R> map) {
    return value.fix(toIO()).map(map);
  }
}

interface IOMonad extends Monad<IO_> {

  IOMonad INSTANCE = new IOMonad() {};

  @Override
  default <T> IO<T> pure(T value) {
    return IO.pure(value);
  }

  @Override
  default <T, R> IO<R> flatMap(Kind<IO_, T> value, Function1<T, ? extends Kind<IO_, R>> map) {
    return value.fix(toIO()).flatMap(map.andThen(IOOf::narrowK));
  }
}

interface IOMonadError extends MonadError<IO_, Throwable>, IOMonad {

  IOMonadError INSTANCE = new IOMonadError() {};

  @Override
  default <A> IO<A> raiseError(Throwable error) {
    return IO.raiseError(error);
  }

  @Override
  default <A> IO<A> handleErrorWith(Kind<IO_, A> value, Function1<Throwable, ? extends Kind<IO_, A>> handler) {
    return IOOf.narrowK(value).redeemWith(handler.andThen(IOOf::narrowK), IO::pure);
  }
}

interface IOMonadThrow extends MonadThrow<IO_>, IOMonadError {

  IOMonadThrow INSTANCE = new IOMonadThrow() {};
}

interface IODefer extends Defer<IO_> {

  @Override
  default <A> IO<A> defer(Producer<Kind<IO_, A>> defer) {
    return IO.suspend(defer.map(IOOf::narrowK));
  }
}

interface IOBracket extends Bracket<IO_, Throwable> {

  @Override
  default <A, B> IO<B> bracket(Kind<IO_, A> acquire, Function1<A, ? extends Kind<IO_, B>> use, Consumer1<A> release) {
    return IO.bracket(acquire.fix(toIO()), use.andThen(IOOf::narrowK), release::accept);
  }
}

interface IOTimer extends Timer<IO_> {
  
  IOTimer INSTANCE = new IOTimer() {};

  @Override
  default IO<Unit> sleep(Duration duration) {
    return IO.sleep(duration);
  }
}

interface IOMonadDefer extends MonadDefer<IO_>, IOMonadError, IODefer, IOBracket, IOTimer {

  IOMonadDefer INSTANCE = new IOMonadDefer() {};
}

final class ConsoleIO implements Console<IO_> {

  public static final ConsoleIO INSTANCE = new ConsoleIO();

  private final SystemConsole console = new SystemConsole();

  @Override
  public IO<String> readln() {
    return IO.task(console::readln);
  }

  @Override
  public IO<Unit> println(String text) {
    return IO.exec(() -> console.println(text));
  }
}