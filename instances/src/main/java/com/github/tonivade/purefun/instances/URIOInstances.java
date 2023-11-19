/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.effect.URIOOf.toURIO;
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
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.URIO;
import com.github.tonivade.purefun.effect.URIOOf;
import com.github.tonivade.purefun.effect.URIO_;
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

  static <R> Functor<Kind<URIO_, R>> functor() {
    return URIOFunctor.INSTANCE;
  }

  static <R> Applicative<Kind<URIO_, R>> applicative() {
    return URIOApplicative.INSTANCE;
  }

  static <R> Monad<Kind<URIO_, R>> monad() {
    return URIOMonad.INSTANCE;
  }

  static <R> MonadThrow<Kind<URIO_, R>> monadThrow() {
    return URIOMonadThrow.INSTANCE;
  }

  static <R> MonadDefer<Kind<URIO_, R>> monadDefer() {
    return URIOMonadDefer.INSTANCE;
  }

  static <R> Async<Kind<URIO_, R>> async() {
    return URIOAsync.INSTANCE;
  }

  static <R> Concurrent<Kind<URIO_, R>> concurrent() {
    return concurrent(Future.DEFAULT_EXECUTOR);
  }

  static <R> Concurrent<Kind<URIO_, R>> concurrent(Executor executor) {
    return URIOConcurrent.instance(executor);
  }

  static <R> Console<Kind<Kind<URIO_, R>, Throwable>> console() {
    return URIOConsole.INSTANCE;
  }
  
  static <R> Runtime<Kind<URIO_, R>> runtime(R env) {
    return URIORuntime.instance(env);
  }
}

interface URIOFunctor<R> extends Functor<Kind<URIO_, R>> {

  @SuppressWarnings("rawtypes")
  URIOFunctor INSTANCE = new URIOFunctor() {};

  @Override
  default <A, B> URIO<R, B> map(Kind<Kind<URIO_, R>, ? extends A> value, 
      Function1<? super A, ? extends B> map) {
    return URIOOf.narrowK(value).map(map);
  }
}

interface URIOPure<R> extends Applicative<Kind<URIO_, R>> {

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
          ap(Kind<Kind<URIO_, R>, ? extends A> value,
             Kind<Kind<URIO_, R>, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(URIOOf::<R, A>narrowK).ap(apply.fix(URIOOf::narrowK));
  }
}

interface URIOMonad<R> extends URIOPure<R>, Monad<Kind<URIO_, R>> {

  @SuppressWarnings("rawtypes")
  URIOMonad INSTANCE = new URIOMonad() {};

  @Override
  default <A, B> URIO<R, B>
          flatMap(Kind<Kind<URIO_, R>, ? extends A> value,
                  Function1<? super A, ? extends Kind<Kind<URIO_, R>, ? extends B>> map) {
    return value.fix(toURIO()).flatMap(map.andThen(URIOOf::narrowK));
  }
}

interface URIOMonadError<R> extends URIOMonad<R>, MonadError<Kind<URIO_, R>, Throwable> {

  @SuppressWarnings("rawtypes")
  URIOMonadError INSTANCE = new URIOMonadError<>() {
  };

  @Override
  default <A> URIO<R, A> raiseError(Throwable error) {
    return URIO.raiseError(error);
  }

  @Override
  default <A> URIO<R, A> handleErrorWith(
      Kind<Kind<URIO_, R>, A> value,
      Function1<? super Throwable, ? extends Kind<Kind<URIO_, R>, ? extends A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<? super Throwable, URIO<R, A>> mapError = handler.andThen(URIOOf::narrowK);
    Function1<A, URIO<R, A>> map = URIO::pure;
    URIO<R, A> urio = URIOOf.narrowK(value);
    return urio.redeemWith(mapError, map);
  }
}

interface URIOMonadThrow<R>
    extends URIOMonadError<R>,
            MonadThrow<Kind<URIO_, R>> {
  @SuppressWarnings("rawtypes")
  URIOMonadThrow INSTANCE = new URIOMonadThrow<>() {
  };
}

interface URIODefer<R> extends Defer<Kind<URIO_, R>> {

  @Override
  default <A> URIO<R, A>
          defer(Producer<? extends Kind<Kind<URIO_, R>, ? extends A>> defer) {
    return URIO.defer(defer::get);
  }
}

interface URIOBracket<R> extends URIOMonadError<R>, Bracket<Kind<URIO_, R>, Throwable> {

  @Override
  default <A, B> URIO<R, B>
          bracket(Kind<Kind<URIO_, R>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<Kind<URIO_, R>, ? extends B>> use,
                  Function1<? super A, ? extends Kind<Kind<URIO_, R>, Unit>> release) {
    return URIO.bracket(acquire, use, release);
  }
}

interface URIOMonadDefer<R>
    extends MonadDefer<Kind<URIO_, R>>, URIODefer<R>, URIOBracket<R> {

  @SuppressWarnings("rawtypes")
  URIOMonadDefer INSTANCE = new URIOMonadDefer<>() {
  };

  @Override
  default URIO<R, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).toURIO();
  }
}

interface URIOAsync<R> extends Async<Kind<URIO_, R>>, URIOMonadDefer<R> {

  @SuppressWarnings("rawtypes")
  URIOAsync INSTANCE = new URIOAsync<>() {
  };
  
  @Override
  default <A> URIO<R, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<Kind<URIO_, R>, Unit>> consumer) {
    return URIO.cancellable((env, cb) -> consumer.andThen(URIOOf::narrowK).apply(cb));
  }
}

interface URIOConcurrent<R> extends URIOAsync<R>, Concurrent<Kind<URIO_, R>> {
  
  static <R> URIOConcurrent<R> instance(Executor executor) {
    return () -> executor;
  }
  
  Executor executor();
  
  @Override
  default <A, B> URIO<R, Either<Tuple2<A, Fiber<Kind<URIO_, R>, B>>, Tuple2<Fiber<Kind<URIO_, R>, A>, B>>> racePair(
    Kind<Kind<URIO_, R>, ? extends A> fa, Kind<Kind<URIO_, R>, ? extends B> fb) {
    return URIO.racePair(executor(), fa, fb);
  }
  
  @Override
  default <A> URIO<R, Fiber<Kind<URIO_, R>, A>> fork(Kind<Kind<URIO_, R>, ? extends A> value) {
    URIO<R, A> fix = value.fix(URIOOf::narrowK);
    return fix.fork();
  }
  
}

final class URIOConsole<R> implements Console<Kind<URIO_, R>> {

  @SuppressWarnings("rawtypes")
  protected static final URIOConsole INSTANCE = new URIOConsole();

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

interface URIORuntime<R> extends Runtime<Kind<URIO_, R>> {
  
  static <R> URIORuntime<R> instance(R env) {
    return () -> env;
  }

  R env();

  @Override
  default <T> T run(Kind<Kind<URIO_, R>, T> value) {
    return value.fix(toURIO()).safeRunSync(env()).getOrElseThrow();
  }
  
  @Override
  default <T> Sequence<T> run(Sequence<Kind<Kind<URIO_, R>, T>> values) {
    return run(URIO.traverse(values.map(URIOOf::<R, T>narrowK)));
  }

  @Override
  default <T> Future<T> parRun(Kind<Kind<URIO_, R>, T> value, Executor executor) {
    return value.fix(toURIO()).runAsync(env());
  }
  
  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<Kind<URIO_, R>, T>> values, Executor executor) {
    return parRun(URIO.traverse(values.map(URIOOf::<R, T>narrowK)), executor);
  }
}