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
import com.github.tonivade.purefun.effect.RIO;
import com.github.tonivade.purefun.effect.RIOOf;
import com.github.tonivade.purefun.effect.UIO;
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

@SuppressWarnings("unchecked")
public interface RIOInstances {

  static <R> Functor<RIO<R, ?>> functor() {
    return RIOFunctor.INSTANCE;
  }

  static <R> Applicative<RIO<R, ?>> applicative() {
    return RIOApplicative.INSTANCE;
  }

  static <R> Monad<RIO<R, ?>> monad() {
    return RIOMonad.INSTANCE;
  }

  static <R> MonadThrow<RIO<R, ?>> monadThrow() {
    return RIOMonadThrow.INSTANCE;
  }

  static <R> MonadDefer<RIO<R, ?>> monadDefer() {
    return RIOMonadDefer.INSTANCE;
  }

  static <R> Async<RIO<R, ?>> async() {
    return RIOAsync.INSTANCE;
  }

  static <R> Concurrent<RIO<R, ?>> concurrent() {
    return concurrent(Future.DEFAULT_EXECUTOR);
  }

  static <R> Concurrent<RIO<R, ?>> concurrent(Executor executor) {
    return RIOConcurrent.instance(executor);
  }

  static <R> Console<Kind<RIO<R, ?>, Throwable>> console() {
    return RIOConsole.INSTANCE;
  }

  static <R> Runtime<RIO<R, ?>> runtime(R env) {
    return RIORuntime.instance(env);
  }
}

interface RIOFunctor<R> extends Functor<RIO<R, ?>> {

  @SuppressWarnings("rawtypes")
  RIOFunctor INSTANCE = new RIOFunctor() {};

  @Override
  default <A, B> RIO<R, B>
          map(Kind<RIO<R, ?>, ? extends A> value, Function1<? super A, ? extends B> map) {
    return RIOOf.toRIO(value).map(map);
  }
}

interface RIOPure<R> extends Applicative<RIO<R, ?>> {

  @Override
  default <A> RIO<R, A> pure(A value) {
    return RIO.pure(value);
  }
}

interface RIOApplicative<R> extends RIOPure<R> {

  @SuppressWarnings("rawtypes")
  RIOApplicative INSTANCE = new RIOApplicative() {};

  @Override
  default <A, B> RIO<R, B>
          ap(Kind<RIO<R, ?>, ? extends A> value,
             Kind<RIO<R, ?>, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(RIOOf::<R, A>toRIO).ap(apply.fix(RIOOf::toRIO));
  }
}

interface RIOMonad<R> extends RIOPure<R>, Monad<RIO<R, ?>> {

  @SuppressWarnings("rawtypes")
  RIOMonad INSTANCE = new RIOMonad() {};

  @Override
  default <A, B> RIO<R, B>
          flatMap(Kind<RIO<R, ?>, ? extends A> value,
                  Function1<? super A, ? extends Kind<RIO<R, ?>, ? extends B>> map) {
    return value.fix(RIOOf::toRIO).flatMap(map.andThen(RIOOf::toRIO));
  }
}

interface RIOMonadError<R> extends RIOMonad<R>, MonadError<RIO<R, ?>, Throwable> {

  @SuppressWarnings("rawtypes")
  RIOMonadError INSTANCE = new RIOMonadError<>() {
  };

  @Override
  default <A> RIO<R, A> raiseError(Throwable error) {
    return RIO.raiseError(error);
  }

  @Override
  default <A> RIO<R, A> handleErrorWith(
      Kind<RIO<R, ?>, A> value,
      Function1<? super Throwable, ? extends Kind<RIO<R, ?>, ? extends A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<? super Throwable, RIO<R, A>> mapError = handler.andThen(RIOOf::toRIO);
    Function1<A, RIO<R, A>> map = RIO::pure;
    RIO<R, A> urio = RIOOf.toRIO(value);
    return urio.foldM(mapError, map);
  }
}

interface RIOMonadThrow<R>
    extends RIOMonadError<R>,
            MonadThrow<RIO<R, ?>> {
  @SuppressWarnings("rawtypes")
  RIOMonadThrow INSTANCE = new RIOMonadThrow<>() {
  };
}

interface RIODefer<R> extends Defer<RIO<R, ?>> {

  @Override
  default <A> RIO<R, A>
          defer(Producer<? extends Kind<RIO<R, ?>, ? extends A>> defer) {
    return RIO.defer(defer::get);
  }
}

interface RIOBracket<R> extends RIOMonadError<R>, Bracket<RIO<R, ?>, Throwable> {

  @Override
  default <A, B> RIO<R, B>
          bracket(Kind<RIO<R, ?>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<RIO<R, ?>, ? extends B>> use,
                  Function1<? super A, ? extends Kind<RIO<R, ?>, Unit>> release) {
    return RIO.bracket(acquire, use, release);
  }
}

interface RIOMonadDefer<R>
    extends MonadDefer<RIO<R, ?>>, RIODefer<R>, RIOBracket<R> {

  @SuppressWarnings("rawtypes")
  RIOMonadDefer INSTANCE = new RIOMonadDefer<>() {
  };

  @Override
  default RIO<R, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).toRIO();
  }
}

interface RIOAsync<R> extends Async<RIO<R, ?>>, RIOMonadDefer<R> {

  @SuppressWarnings("rawtypes")
  RIOAsync INSTANCE = new RIOAsync<>() {
  };

  @Override
  default <A> RIO<R, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<RIO<R, ?>, Unit>> consumer) {
    return RIO.cancellable((env, cb) -> consumer.andThen(RIOOf::toRIO).apply(cb));
  }
}

interface RIOConcurrent<R> extends RIOAsync<R>, Concurrent<RIO<R, ?>> {

  static <R> RIOConcurrent<R> instance(Executor executor) {
    return () -> executor;
  }

  Executor executor();

  @Override
  default <A, B> RIO<R, Either<Tuple2<A, Fiber<RIO<R, ?>, B>>, Tuple2<Fiber<RIO<R, ?>, A>, B>>> racePair(
    Kind<RIO<R, ?>, ? extends A> fa, Kind<RIO<R, ?>, ? extends B> fb) {
    return RIO.racePair(executor(), fa, fb);
  }

  @Override
  default <A> RIO<R, Fiber<RIO<R, ?>, A>> fork(Kind<RIO<R, ?>, ? extends A> value) {
    RIO<R, A> fix = value.fix(RIOOf::toRIO);
    return fix.fork();
  }
}

final class RIOConsole<R> implements Console<RIO<R, ?>> {

  @SuppressWarnings("rawtypes")
  static final RIOConsole INSTANCE = new RIOConsole();

  private final SystemConsole console = new SystemConsole();

  @Override
  public RIO<R, String> readln() {
    return RIO.task(console::readln);
  }

  @Override
  public RIO<R, Unit> println(String text) {
    return RIO.exec(() -> console.println(text));
  }
}

interface RIORuntime<R> extends Runtime<RIO<R, ?>> {

  static <R> RIORuntime<R> instance(R env) {
    return () -> env;
  }

  R env();

  @Override
  default <T> T run(Kind<RIO<R, ?>, T> value) {
    return value.fix(RIOOf::toRIO).safeRunSync(env()).getOrElseThrow();
  }

  @Override
  default <T> Sequence<T> run(Sequence<Kind<RIO<R, ?>, T>> values) {
    return run(RIO.traverse(values.map(RIOOf::<R, T>toRIO)));
  }

  @Override
  default <T> Future<T> parRun(Kind<RIO<R, ?>, T> value, Executor executor) {
    return value.fix(RIOOf::<R, T>toRIO).runAsync(env());
  }

  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<RIO<R, ?>, T>> values, Executor executor) {
    return parRun(RIO.traverse(values.map(RIOOf::<R, T>toRIO)), executor);
  }
}