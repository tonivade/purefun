/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.effect.PureIOOf.toPureIO;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.effect.PureIOOf;
import com.github.tonivade.purefun.effect.PureIO_;
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
public interface PureIOInstances {

  static <R, E> Functor<Kind<Kind<PureIO_, R>, E>> functor() {
    return PureIOFunctor.INSTANCE;
  }

  static <R, E> Applicative<Kind<Kind<PureIO_, R>, E>> applicative() {
    return PureIOApplicative.INSTANCE;
  }

  static <R, E> Monad<Kind<Kind<PureIO_, R>, E>> monad() {
    return PureIOMonad.INSTANCE;
  }

  static <R, E> MonadError<Kind<Kind<PureIO_, R>, E>, E> monadError() {
    return PureIOMonadError.INSTANCE;
  }

  static <R> MonadThrow<Kind<Kind<PureIO_, R>, Throwable>> monadThrow() {
    return PureIOMonadThrow.INSTANCE;
  }

  static <R> MonadDefer<Kind<Kind<PureIO_, R>, Throwable>> monadDefer() {
    return PureIOMonadDefer.INSTANCE;
  }

  static <R> Async<Kind<Kind<PureIO_, R>, Throwable>> async() {
    return PureIOAsync.INSTANCE;
  }

  static <R> Concurrent<Kind<Kind<PureIO_, R>, Throwable>> concurrent() {
    return concurrent(Future.DEFAULT_EXECUTOR);
  }

  static <R> Concurrent<Kind<Kind<PureIO_, R>, Throwable>> concurrent(Executor executor) {
    return PureIOConcurrent.instance(executor);
  }

  static <R> Console<Kind<Kind<PureIO_, R>, Throwable>> console() {
    return PureIOConsole.INSTANCE;
  }
  
  static <R, E> Runtime<Kind<Kind<PureIO_, R>, E>> runtime(R env) {
    return PureIORuntime.instance(env);
  }
}

interface PureIOFunctor<R, E> extends Functor<Kind<Kind<PureIO_, R>, E>> {

  @SuppressWarnings("rawtypes")
  PureIOFunctor INSTANCE = new PureIOFunctor() {};

  @Override
  default <A, B> PureIO<R, E, B> map(
      Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> value, 
      Function1<? super A, ? extends B> map) {
    return PureIOOf.narrowK(value).map(map);
  }
}

interface PureIOPure<R, E> extends Applicative<Kind<Kind<PureIO_, R>, E>> {

  @Override
  default <A> PureIO<R, E, A> pure(A value) {
    return PureIO.pure(value);
  }
}

interface PureIOApplicative<R, E> extends PureIOPure<R, E> {

  @SuppressWarnings("rawtypes")
  PureIOApplicative INSTANCE = new PureIOApplicative() {};

  @Override
  default <A, B> PureIO<R, E, B>
          ap(Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> value,
             Kind<Kind<Kind<PureIO_, R>, E>, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(PureIOOf::<R, E, A>narrowK).ap(apply.fix(PureIOOf::narrowK));
  }
}

interface PureIOMonad<R, E> extends PureIOPure<R, E>, Monad<Kind<Kind<PureIO_, R>, E>> {

  @SuppressWarnings("rawtypes")
  PureIOMonad INSTANCE = new PureIOMonad() {};

  @Override
  default <A, B> PureIO<R, E, B>
          flatMap(Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> value,
                  Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends B>> map) {
    return value.fix(toPureIO()).flatMap(map.andThen(PureIOOf::narrowK));
  }
}

interface PureIOMonadError<R, E> extends PureIOMonad<R, E>, MonadError<Kind<Kind<PureIO_, R>, E>, E> {

  @SuppressWarnings("rawtypes")
  PureIOMonadError INSTANCE = new PureIOMonadError() {};

  @Override
  default <A> PureIO<R, E, A> raiseError(E error) {
    return PureIO.raiseError(error);
  }

  @Override
  default <A> PureIO<R, E, A> handleErrorWith(
      Kind<Kind<Kind<PureIO_, R>, E>, A> value,
      Function1<? super E, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<? super E, PureIO<R, E, A>> mapError = handler.andThen(PureIOOf::narrowK);
    Function1<A, PureIO<R, E, A>> map = PureIO::pure;
    PureIO<R, E, A> PureIO = PureIOOf.narrowK(value);
    return PureIO.foldM(mapError, map);
  }
}

interface PureIOMonadThrow<R>
    extends PureIOMonadError<R, Throwable>, MonadThrow<Kind<Kind<PureIO_, R>, Throwable>> {
  @SuppressWarnings("rawtypes")
  PureIOMonadThrow INSTANCE = new PureIOMonadThrow() {};
}

interface PureIODefer<R, E> extends Defer<Kind<Kind<PureIO_, R>, E>> {

  @Override
  default <A> PureIO<R, E, A>
          defer(Producer<? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends A>> defer) {
    return PureIO.defer(() -> defer.map(PureIOOf::<R, E, A>narrowK).get());
  }
}

interface PureIOBracket<R, E> extends PureIOMonadError<R, E>, Bracket<Kind<Kind<PureIO_, R>, E>, E> {

  @Override
  default <A, B> PureIO<R, E, B>
          bracket(Kind<Kind<Kind<PureIO_, R>, E>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, E>, ? extends B>> use,
                  Function1<? super A, ? extends Kind<Kind<Kind<PureIO_, R>, E>, Unit>> release) {
    return PureIO.bracket(acquire, use, release);
  }
}

interface PureIOMonadDefer<R>
    extends MonadDefer<Kind<Kind<PureIO_, R>, Throwable>>, PureIODefer<R, Throwable>, PureIOBracket<R, Throwable> {

  @SuppressWarnings("rawtypes")
  PureIOMonadDefer INSTANCE = new PureIOMonadDefer() {};

  @Override
  default PureIO<R, Throwable, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).toPureIO();
  }
}

interface PureIOAsync<R> extends Async<Kind<Kind<PureIO_, R>, Throwable>>, PureIOMonadDefer<R> {

  @SuppressWarnings("rawtypes")
  PureIOAsync INSTANCE = new PureIOAsync<>() {
  };
  
  @Override
  default <A> PureIO<R, Throwable, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<Kind<Kind<PureIO_, R>, Throwable>, Unit>> consumer) {
    return PureIO.cancellable((env, cb) -> consumer.andThen(PureIOOf::narrowK).apply(e -> cb.accept(Try.success(e.toEither()))));
  }
}

interface PureIOConcurrent<R> extends Concurrent<Kind<Kind<PureIO_, R>, Throwable>>, PureIOAsync<R> {
  
  static <R> PureIOConcurrent<R> instance(Executor executor) {
    return () -> executor;
  }
  
  Executor executor();

  @Override
  default <A, B> PureIO<R, Throwable, Either<Tuple2<A, Fiber<Kind<Kind<PureIO_, R>, Throwable>, B>>, Tuple2<Fiber<Kind<Kind<PureIO_, R>, Throwable>, A>, B>>> racePair(
      Kind<Kind<Kind<PureIO_, R>, Throwable>, ? extends A> fa, Kind<Kind<Kind<PureIO_, R>, Throwable>, ? extends B> fb) {
    return PureIO.racePair(executor(), fa, fb);
  }
  
  @Override
  default <A> PureIO<R, Throwable, Fiber<Kind<Kind<PureIO_, R>, Throwable>, A>> fork(Kind<Kind<Kind<PureIO_, R>, Throwable>, ? extends A> value) {
    PureIO<R, Throwable, A> fix = value.fix(PureIOOf::narrowK);
    return fix.fork();
  }
}

final class PureIOConsole<R> implements Console<Kind<Kind<PureIO_, R>, Throwable>> {

  @SuppressWarnings("rawtypes")
  static final PureIOConsole INSTANCE = new PureIOConsole();

  private final SystemConsole console = new SystemConsole();

  @Override
  public PureIO<R, Throwable, String> readln() {
    return PureIO.task(console::readln);
  }

  @Override
  public PureIO<R, Throwable, Unit> println(String text) {
    return PureIO.exec(() -> console.println(text));
  }
}

interface PureIORuntime<R, E> extends Runtime<Kind<Kind<PureIO_, R>, E>> {
  
  static <R, E> PureIORuntime<R, E> instance(R env) {
    return () -> env;
  }

  R env();

  @Override
  default <T> T run(Kind<Kind<Kind<PureIO_, R>, E>, T> value) {
    return value.fix(toPureIO()).provide(env()).getRight();
  }
  
  @Override
  default <T> Sequence<T> run(Sequence<Kind<Kind<Kind<PureIO_, R>, E>, T>> values) {
    return run(PureIO.traverse(values.map(PureIOOf::<R, E, T>narrowK)));
  }

  @Override
  default <T> Future<T> parRun(Kind<Kind<Kind<PureIO_, R>, E>, T> value, Executor executor) {
    return value.fix(toPureIO()).runAsync(env()).map(Either::get);
  }
  
  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<Kind<Kind<PureIO_, R>, E>, T>> values, Executor executor) {
    return parRun(PureIO.traverse(values.map(PureIOOf::<R, E, T>narrowK)), executor);
  }
}