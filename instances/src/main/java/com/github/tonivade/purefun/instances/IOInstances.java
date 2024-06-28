/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.time.Duration;
import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
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

  static Functor<IO<?>> functor() {
    return IOFunctor.INSTANCE;
  }

  static Applicative<IO<?>> applicative() {
    return applicative(Future.DEFAULT_EXECUTOR);
  }

  static Applicative<IO<?>> applicative(Executor executor) {
    return IOApplicative.instance(executor);
  }

  static Monad<IO<?>> monad() {
    return IOMonad.INSTANCE;
  }

  static MonadError<IO<?>, Throwable> monadError() {
    return IOMonadError.INSTANCE;
  }

  static MonadThrow<IO<?>> monadThrow() {
    return IOMonadThrow.INSTANCE;
  }

  static Timer<IO<?>> timer() {
    return IOMonadDefer.INSTANCE;
  }

  static MonadDefer<IO<?>> monadDefer() {
    return IOMonadDefer.INSTANCE;
  }

  static Async<IO<?>> async() {
    return IOAsync.INSTANCE;
  }

  static Concurrent<IO<?>> concurrent() {
    return concurrent(Future.DEFAULT_EXECUTOR);
  }

  static Concurrent<IO<?>> concurrent(Executor executor) {
    return IOConcurrent.instance(executor);
  }

  static Console<IO<?>> console() {
    return IOConsole.INSTANCE;
  }

  static Runtime<IO<?>> runtime() {
    return IORuntime.INSTANCE;
  }
}

interface IOFunctor extends Functor<IO<?>> {

  IOFunctor INSTANCE = new IOFunctor() {};

  @Override
  default <T, R> Kind<IO<?>, R> map(Kind<IO<?>, ? extends T> value, Function1<? super T, ? extends R> map) {
    return value.fix(IOOf::toIO).map(map);
  }
}

interface IOPure extends Applicative<IO<?>> {

  @Override
  default <T> IO<T> pure(T value) {
    return IO.pure(value);
  }
}

interface IOApplicative extends IOPure, Applicative<IO<?>> {

  static IOApplicative instance(Executor executor) {
    return () -> executor;
  }

  Executor executor();

  @Override
  default <T, R> IO<R> ap(Kind<IO<?>, ? extends T> value,
      Kind<IO<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return IO.parMap2(executor(), value.fix(IOOf::toIO), apply.fix(IOOf::toIO), (v, a) -> a.apply(v));
  }
}

interface IOMonad extends Monad<IO<?>>, IOPure {

  IOMonad INSTANCE = new IOMonad() {};

  @Override
  default <T, R> IO<R> flatMap(
      Kind<IO<?>, ? extends T> value,
      Function1<? super T, ? extends Kind<IO<?>, ? extends R>> map) {
    return value.fix(IOOf::toIO).flatMap(map.andThen(IOOf::toIO));
  }
}

interface IOMonadError extends MonadError<IO<?>, Throwable>, IOMonad {

  IOMonadError INSTANCE = new IOMonadError() {};

  @Override
  default <A> IO<A> raiseError(Throwable error) {
    return IO.raiseError(error);
  }

  @Override
  default <A> IO<A> handleErrorWith(
      Kind<IO<?>, A> value,
      Function1<? super Throwable, ? extends Kind<IO<?>, ? extends A>> handler) {
    return IOOf.toIO(value).redeemWith(handler.andThen(IOOf::toIO), IO::pure);
  }
}

interface IOMonadThrow extends MonadThrow<IO<?>>, IOMonadError {

  IOMonadThrow INSTANCE = new IOMonadThrow() {};
}

interface IODefer extends Defer<IO<?>> {

  @Override
  default <A> IO<A> defer(Producer<? extends Kind<IO<?>, ? extends A>> defer) {
    return IO.suspend(defer.map(IOOf::toIO));
  }
}

interface IOBracket extends IOMonadError, Bracket<IO<?>, Throwable> {

  @Override
  default <A, B> IO<B> bracket(
      Kind<IO<?>, ? extends A> acquire,
      Function1<? super A, ? extends Kind<IO<?>, ? extends B>> use,
      Function1<? super A, ? extends Kind<IO<?>, Unit>> release) {
    return IO.bracket(acquire, use, release);
  }
}

interface IOMonadDefer extends MonadDefer<IO<?>>, IODefer, IOBracket {

  IOMonadDefer INSTANCE = new IOMonadDefer() {};

  @Override
  default IO<Unit> sleep(Duration duration) {
    return IO.sleep(duration);
  }
}

interface IOAsync extends Async<IO<?>>, IOMonadDefer {

  IOAsync INSTANCE = new IOAsync() {};

  @Override
  default <A> IO<A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<IO<?>, Unit>> consumer) {
    return IO.cancellable(consumer.andThen(IOOf::toIO));
  }
}

interface IOConcurrent extends Concurrent<IO<?>>, IOAsync {

  static IOConcurrent instance(Executor executor) {
    return () -> executor;
  }

  Executor executor();

  @Override
  default <A, B> IO<Either<Tuple2<A, Fiber<IO<?>, B>>, Tuple2<Fiber<IO<?>, A>, B>>> racePair(Kind<IO<?>, ? extends A> fa, Kind<IO<?>, ? extends B> fb) {
    return IO.racePair(executor(), fa, fb);
  }

  @Override
  default <A> IO<Fiber<IO<?>, A>> fork(Kind<IO<?>, ? extends A> value) {
    Kind<IO<?>, A> narrowK = Kind.narrowK(value);
    IO<A> fix = narrowK.fix(IOOf::toIO);
    return fix.fork();
  }
}

final class IOConsole implements Console<IO<?>> {

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

interface IORuntime extends Runtime<IO<?>> {

  IORuntime INSTANCE = new IORuntime() {};

  @Override
  default <T> T run(Kind<IO<?>, T> value) {
    return value.fix(IOOf::toIO).unsafeRunSync();
  }

  @Override
  default <T> Sequence<T> run(Sequence<Kind<IO<?>, T>> values) {
    return run(IO.traverse(values.map(IOOf::<T>toIO)));
  }

  @Override
  default <T> Future<T> parRun(Kind<IO<?>, T> value, Executor executor) {
    return value.fix(IOOf::<T>toIO).runAsync(executor);
  }

  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<IO<?>, T>> values, Executor executor) {
    return parRun(IO.traverse(values.map(IOOf::<T>toIO)), executor);
  }
}