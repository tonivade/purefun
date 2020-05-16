/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.time.Duration;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;

public interface IOInstances {

  static Functor<IO_> functor() {
    return IOFunctor.instance();
  }

  static Monad<IO_> monad() {
    return IOMonad.instance();
  }

  static MonadError<IO_, Throwable> monadError() {
    return IOMonadError.instance();
  }

  static MonadThrow<IO_> monadThrow() {
    return IOMonadThrow.instance();
  }

  static MonadDefer<IO_> monadDefer() {
    return IOMonadDefer.instance();
  }

  static <A> Reference<IO_, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }

  static Console<IO_> console() {
    return ConsoleIO.INSTANCE;
  }
}

@Instance
interface IOFunctor extends Functor<IO_> {

  @Override
  default <T, R> Higher1<IO_, R> map(Higher1<IO_, T> value, Function1<T, R> map) {
    return IO_.narrowK(value).map(map);
  }
}

@Instance
interface IOMonad extends Monad<IO_> {

  @Override
  default <T> Higher1<IO_, T> pure(T value) {
    return IO.pure(value);
  }

  @Override
  default <T, R> Higher1<IO_, R> flatMap(Higher1<IO_, T> value, Function1<T, ? extends Higher1<IO_, R>> map) {
    return IO_.narrowK(value).flatMap(map.andThen(IO_::narrowK));
  }
}

@Instance
interface IOMonadError extends MonadError<IO_, Throwable>, IOMonad {

  @Override
  default <A> Higher1<IO_, A> raiseError(Throwable error) {
    return IO.raiseError(error);
  }

  @Override
  default <A> Higher1<IO_, A> handleErrorWith(Higher1<IO_, A> value, Function1<Throwable, ? extends Higher1<IO_, A>> handler) {
    return IO_.narrowK(value).redeemWith(handler.andThen(IO_::narrowK), IO::pure);
  }
}

@Instance
interface IOMonadThrow extends MonadThrow<IO_>, IOMonadError { }

interface IODefer extends Defer<IO_> {

  @Override
  default <A> Higher1<IO_, A> defer(Producer<Higher1<IO_, A>> defer) {
    return IO.suspend(defer.map(IO_::narrowK));
  }
}

interface IOBracket extends Bracket<IO_> {

  @Override
  default <A, B> Higher1<IO_, B> bracket(Higher1<IO_, A> acquire, Function1<A, ? extends Higher1<IO_, B>> use, Consumer1<A> release) {
    return IO.bracket(IO_.narrowK(acquire), use.andThen(IO_::narrowK), release::accept);
  }
}

@Instance
interface IOMonadDefer extends MonadDefer<IO_>, IOMonadError, IODefer, IOBracket {

  @Override
  default Higher1<IO_, Unit> sleep(Duration duration) {
    return IO.sleep(duration);
  }
}

final class ConsoleIO implements Console<IO_> {

  public static final ConsoleIO INSTANCE = new ConsoleIO();

  private final SystemConsole console = new SystemConsole();

  @Override
  public Higher1<IO_, String> readln() {
    return IO.task(console::readln);
  }

  @Override
  public Higher1<IO_, Unit> println(String text) {
    return IO.exec(() -> console.println(text));
  }
}

final class SystemConsole {

  protected void println(String message) {
    writer().println(message);
  }

  protected String readln() {
    try {
      return reader().readLine();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private BufferedReader reader() {
    return new BufferedReader(new InputStreamReader(System.in));
  }

  private PrintWriter writer() {
    return new PrintWriter(System.out, true);
  }
}