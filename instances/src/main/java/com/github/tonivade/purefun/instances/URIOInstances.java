/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
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
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.URIO;
import com.github.tonivade.purefun.effect.URIOOf;
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
public interface URIOInstances {

  static <R> Functor<URIO<R, ?>> functor() {
    return URIOFunctor.INSTANCE;
  }

  static <R> Applicative<URIO<R, ?>> applicative() {
    return URIOApplicative.INSTANCE;
  }

  static <R> Monad<URIO<R, ?>> monad() {
    return URIOMonad.INSTANCE;
  }

  static <R> MonadThrow<URIO<R, ?>> monadThrow() {
    return URIOMonadThrow.INSTANCE;
  }

  static <R> MonadDefer<URIO<R, ?>> monadDefer() {
    return URIOMonadDefer.INSTANCE;
  }

  static <R> Async<URIO<R, ?>> async() {
    return URIOAsync.INSTANCE;
  }

  static <R> Concurrent<URIO<R, ?>> concurrent() {
    return concurrent(Future.DEFAULT_EXECUTOR);
  }

  static <R> Concurrent<URIO<R, ?>> concurrent(Executor executor) {
    return URIOConcurrent.instance(executor);
  }

  static <R> Console<URIO<R, ?>> console() {
    return URIOConsole.INSTANCE;
  }

  static <R> Runtime<URIO<R, ?>> runtime(R env) {
    return URIORuntime.instance(env);
  }
}

interface URIOFunctor<R> extends Functor<URIO<R, ?>> {

  @SuppressWarnings("rawtypes")
  URIOFunctor INSTANCE = new URIOFunctor() {};

  @Override
  default <A, B> URIO<R, B> map(Kind<URIO<R, ?>, ? extends A> value,
      Function1<? super A, ? extends B> map) {
    return URIOOf.toURIO(value).map(map);
  }
}

interface URIOPure<R> extends Applicative<URIO<R, ?>> {

  @Override
  default <A> URIO<R, A> pure(A value) {
    return URIO.pure(value);
  }
}

interface URIOApplicative<R> extends URIOPure<R> {

  @SuppressWarnings("rawtypes")
  URIOApplicative INSTANCE = new URIOApplicative() {};

  @Override
  default <A, B> URIO<R, B>
          ap(Kind<URIO<R, ?>, ? extends A> value,
             Kind<URIO<R, ?>, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(URIOOf::<R, A>toURIO).ap(apply.fix(URIOOf::toURIO));
  }
}

interface URIOMonad<R> extends URIOPure<R>, Monad<URIO<R, ?>> {

  @SuppressWarnings("rawtypes")
  URIOMonad INSTANCE = new URIOMonad() {};

  @Override
  default <A, B> URIO<R, B>
          flatMap(Kind<URIO<R, ?>, ? extends A> value,
                  Function1<? super A, ? extends Kind<URIO<R, ?>, ? extends B>> map) {
    return value.fix(URIOOf::toURIO).flatMap(map.andThen(URIOOf::toURIO));
  }
}

interface URIOMonadError<R> extends URIOMonad<R>, MonadError<URIO<R, ?>, Throwable> {

  @SuppressWarnings("rawtypes")
  URIOMonadError INSTANCE = new URIOMonadError<>() {
  };

  @Override
  default <A> URIO<R, A> raiseError(Throwable error) {
    return URIO.raiseError(error);
  }

  @Override
  default <A> URIO<R, A> handleErrorWith(
      Kind<URIO<R, ?>, A> value,
      Function1<? super Throwable, ? extends Kind<URIO<R, ?>, ? extends A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<? super Throwable, URIO<R, A>> mapError = handler.andThen(URIOOf::toURIO);
    Function1<A, URIO<R, A>> map = URIO::pure;
    URIO<R, A> urio = URIOOf.toURIO(value);
    return urio.redeemWith(mapError, map);
  }
}

interface URIOMonadThrow<R>
    extends URIOMonadError<R>,
            MonadThrow<URIO<R, ?>> {
  @SuppressWarnings("rawtypes")
  URIOMonadThrow INSTANCE = new URIOMonadThrow<>() {
  };
}

interface URIODefer<R> extends Defer<URIO<R, ?>> {

  @Override
  default <A> URIO<R, A>
          defer(Producer<? extends Kind<URIO<R, ?>, ? extends A>> defer) {
    return URIO.defer(defer::get);
  }
}

interface URIOBracket<R> extends URIOMonadError<R>, Bracket<URIO<R, ?>, Throwable> {

  @Override
  default <A, B> URIO<R, B>
          bracket(Kind<URIO<R, ?>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<URIO<R, ?>, ? extends B>> use,
                  Function1<? super A, ? extends Kind<URIO<R, ?>, Unit>> release) {
    return URIO.bracket(acquire, use, release);
  }
}

interface URIOMonadDefer<R>
    extends MonadDefer<URIO<R, ?>>, URIODefer<R>, URIOBracket<R> {

  @SuppressWarnings("rawtypes")
  URIOMonadDefer INSTANCE = new URIOMonadDefer<>() {
  };

  @Override
  default URIO<R, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).toURIO();
  }
}

interface URIOAsync<R> extends Async<URIO<R, ?>>, URIOMonadDefer<R> {

  @SuppressWarnings("rawtypes")
  URIOAsync INSTANCE = new URIOAsync<>() {
  };

  @Override
  default <A> URIO<R, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<URIO<R, ?>, Unit>> consumer) {
    return URIO.cancellable((env, cb) -> consumer.andThen(URIOOf::toURIO).apply(cb));
  }
}

interface URIOConcurrent<R> extends URIOAsync<R>, Concurrent<URIO<R, ?>> {

  static <R> URIOConcurrent<R> instance(Executor executor) {
    return () -> executor;
  }

  Executor executor();

  @Override
  default <A, B> URIO<R, Either<Tuple2<A, Fiber<URIO<R, ?>, B>>, Tuple2<Fiber<URIO<R, ?>, A>, B>>> racePair(
    Kind<URIO<R, ?>, ? extends A> fa, Kind<URIO<R, ?>, ? extends B> fb) {
    return URIO.racePair(executor(), fa, fb);
  }

  @Override
  default <A> URIO<R, Fiber<URIO<R, ?>, A>> fork(Kind<URIO<R, ?>, ? extends A> value) {
    URIO<R, A> fix = value.fix(URIOOf::toURIO);
    return fix.fork();
  }

}

final class URIOConsole<R> implements Console<URIO<R, ?>> {

  @SuppressWarnings("rawtypes")
  static final URIOConsole INSTANCE = new URIOConsole();

  private final SystemConsole console = new SystemConsole();

  @Override
  public URIO<R, String> readln() {
    return URIO.task(console::readln);
  }

  @Override
  public URIO<R, Unit> println(String text) {
    return URIO.exec(() -> console.println(text));
  }
}

interface URIORuntime<R> extends Runtime<URIO<R, ?>> {

  static <R> URIORuntime<R> instance(R env) {
    return () -> env;
  }

  R env();

  @Override
  default <T> T run(Kind<URIO<R, ?>, T> value) {
    return value.fix(URIOOf::toURIO).safeRunSync(env()).getOrElseThrow();
  }

  @Override
  default <T> Sequence<T> run(Sequence<Kind<URIO<R, ?>, T>> values) {
    return run(URIO.traverse(values.map(URIOOf::<R, T>toURIO)));
  }

  @Override
  default <T> Future<T> parRun(Kind<URIO<R, ?>, T> value, Executor executor) {
    return value.fix(URIOOf::<R, T>toURIO).runAsync(env());
  }

  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<URIO<R, ?>, T>> values, Executor executor) {
    return parRun(URIO.traverse(values.map(URIOOf::<R, T>toURIO)), executor);
  }
}