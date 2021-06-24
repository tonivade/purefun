/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.monad.IOOf.toIO;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Async;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Concurrent;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Runtime;
import com.github.tonivade.purefun.typeclasses.Timer;

public interface IOInstances {

  static Functor<IO_> functor() {
    return IOFunctor.INSTANCE;
  }

  static Applicative<IO_> applicative() {
    return applicative(Future.DEFAULT_EXECUTOR);
  }

  static Applicative<IO_> applicative(Executor executor) {
    return IOApplicative.instance(executor);
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

  static Async<IO_> async() {
    return IOAsync.INSTANCE;
  }

  static Concurrent<IO_> concurrent() {
    return concurrent(Future.DEFAULT_EXECUTOR);
  }

  static Concurrent<IO_> concurrent(Executor executor) {
    return IOConcurrent.instance(executor);
  }

  static Console<IO_> console() {
    return IOConsole.INSTANCE;
  }
  
  static Runtime<IO_> runtime() {
    return IORuntime.INSTANCE;
  }
}

interface IOFunctor extends Functor<IO_> {

  IOFunctor INSTANCE = new IOFunctor() {};

  @Override
  default <T, R> Kind<IO_, R> map(Kind<IO_, ? extends T> value, Function1<? super T, ? extends R> map) {
    return value.fix(toIO()).map(map);
  }
}

interface IOPure extends Applicative<IO_> {

  @Override
  default <T> IO<T> pure(T value) {
    return IO.pure(value);
  }
}

interface IOApplicative extends IOPure, Applicative<IO_> {
  
  static IOApplicative instance(Executor executor) {
    return () -> executor;
  }

  Executor executor();

  @Override
  default <T, R> IO<R> ap(Kind<IO_, ? extends T> value, 
      Kind<IO_, ? extends Function1<? super T, ? extends R>> apply) {
    return IO.parMap2(executor(), value.fix(toIO()), apply.fix(toIO()), (v, a) -> a.apply(v));
  }
}

interface IOMonad extends Monad<IO_>, IOPure {

  IOMonad INSTANCE = new IOMonad() {};

  @Override
  default <T, R> IO<R> flatMap(
      Kind<IO_, ? extends T> value, 
      Function1<? super T, ? extends Kind<IO_, ? extends R>> map) {
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
  default <A> IO<A> handleErrorWith(
      Kind<IO_, A> value, 
      Function1<? super Throwable, ? extends Kind<IO_, ? extends A>> handler) {
    return IOOf.narrowK(value).redeemWith(handler.andThen(IOOf::narrowK), IO::pure);
  }
}

interface IOMonadThrow extends MonadThrow<IO_>, IOMonadError {

  IOMonadThrow INSTANCE = new IOMonadThrow() {};
}

interface IODefer extends Defer<IO_> {

  @Override
  default <A> IO<A> defer(Producer<? extends Kind<IO_, ? extends A>> defer) {
    return IO.suspend(defer.map(IOOf::narrowK));
  }
}

interface IOBracket extends IOMonadError, Bracket<IO_, Throwable> {

  @Override
  default <A, B> IO<B> bracket(
      Kind<IO_, ? extends A> acquire, 
      Function1<? super A, ? extends Kind<IO_, ? extends B>> use, 
      Function1<? super A, ? extends Kind<IO_, Unit>> release) {
    return IO.bracket(acquire, use, release);
  }
}

interface IOMonadDefer extends MonadDefer<IO_>, IODefer, IOBracket {

  IOMonadDefer INSTANCE = new IOMonadDefer() {};

  @Override
  default IO<Unit> sleep(Duration duration) {
    return IO.sleep(duration);
  }
}

interface IOAsync extends Async<IO_>, IOMonadDefer {

  IOAsync INSTANCE = new IOAsync() {};
  
  @Override
  default <A> IO<A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<IO_, Unit>> consumer) {
    return IO.cancellable(consumer.andThen(IOOf::narrowK));
  }
}

interface IOConcurrent extends Concurrent<IO_>, IOAsync {
  
  static IOConcurrent instance(Executor executor) {
    return () -> executor;
  }
  
  Executor executor();
  
  @Override
  default <A, B> IO<Either<Tuple2<A, Fiber<IO_, B>>, Tuple2<Fiber<IO_, A>, B>>> racePair(Kind<IO_, A> fa, Kind<IO_, B> fb) {
    return IO.racePair(executor(), fa.fix(toIO()), fb.fix(toIO()));
  }
  
  @Override
  default <A> IO<Fiber<IO_, A>> fork(Kind<IO_, A> value) {
    return value.fix(toIO()).fork();
  }
}

final class IOConsole implements Console<IO_> {

  public static final IOConsole INSTANCE = new IOConsole();

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

interface IORuntime extends Runtime<IO_> {
  
  IORuntime INSTANCE = new IORuntime() {};

  @Override
  default <T> T run(Kind<IO_, T> value) {
    return value.fix(toIO()).unsafeRunSync();
  }
  
  @Override
  default <T> Sequence<T> run(Sequence<Kind<IO_, T>> values) {
    return run(IO.traverse(values.map(IOOf::<T>narrowK)));
  }

  @Override
  default <T> Future<T> parRun(Kind<IO_, T> value, Executor executor) {
    return value.fix(toIO()).runAsync();
  }
  
  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<IO_, T>> values, Executor executor) {
    return parRun(IO.traverse(values.map(IOOf::<T>narrowK)), executor);
  }
}