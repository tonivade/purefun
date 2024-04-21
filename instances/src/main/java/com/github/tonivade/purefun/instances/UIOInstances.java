/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.effect.UIOOf.toUIO;
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
import com.github.tonivade.purefun.effect.UIOOf;
import com.github.tonivade.purefun.effect.UIO_;
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

public interface UIOInstances {

  static Functor<UIO_> functor() {
    return UIOFunctor.INSTANCE;
  }

  static Applicative<UIO_> applicative() {
    return UIOApplicative.INSTANCE;
  }

  static Monad<UIO_> monad() {
    return UIOMonad.INSTANCE;
  }

  static MonadError<UIO_, Throwable> monadError() {
    return UIOMonadError.INSTANCE;
  }

  static MonadThrow<UIO_> monadThrow() {
    return UIOMonadThrow.INSTANCE;
  }

  static MonadDefer<UIO_> monadDefer() {
    return UIOMonadDefer.INSTANCE;
  }

  static Async<UIO_> async() {
    return UIOAsync.INSTANCE;
  }

  static Concurrent<UIO_> concurrent() {
    return concurrent(Future.DEFAULT_EXECUTOR);
  }

  static Concurrent<UIO_> concurrent(Executor executor) {
    return UIOConcurrent.instance(executor);
  }
  
  static Runtime<UIO_> runtime() {
    return UIORuntime.INSTANCE;
  }
  
  static Console<UIO_> console() {
    return UIOConsole.INSTANCE;
  }
}

interface UIOFunctor extends Functor<UIO_> {

  UIOFunctor INSTANCE = new UIOFunctor() {};

  @Override
  default <A, B> UIO<B> map(Kind<UIO_, ? extends A> value, Function1<? super A, ? extends B> map) {
    return UIOOf.narrowK(value).map(map);
  }
}

interface UIOPure extends Applicative<UIO_> {

  @Override
  default <A> UIO<A> pure(A value) {
    return UIO.pure(value);
  }
}

interface UIOApplicative extends UIOPure {

  UIOApplicative INSTANCE = new UIOApplicative() {};

  @Override
  default <A, B> UIO<B> ap(Kind<UIO_, ? extends A> value, 
      Kind<UIO_, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(UIOOf::<A>narrowK).ap(apply.fix(UIOOf::narrowK));
  }
}

interface UIOMonad extends UIOPure, Monad<UIO_> {

  UIOMonad INSTANCE = new UIOMonad() {};

  @Override
  default <A, B> UIO<B> flatMap(Kind<UIO_, ? extends A> value, 
      Function1<? super A, ? extends Kind<UIO_, ? extends B>> map) {
    return value.fix(toUIO()).flatMap(map.andThen(UIOOf::narrowK));
  }
}

interface UIOMonadError extends UIOMonad, MonadError<UIO_, Throwable> {

  UIOMonadError INSTANCE = new UIOMonadError() {};

  @Override
  default <A> UIO<A> raiseError(Throwable error) {
    return UIO.raiseError(error);
  }

  @Override
  default <A> UIO<A> handleErrorWith(
      Kind<UIO_, A> value,
      Function1<? super Throwable, ? extends Kind<UIO_, ? extends A>> handler) {
    Function1<? super Throwable, UIO<A>> mapError = handler.andThen(UIOOf::narrowK);
    Function1<A, UIO<A>> map = UIO::pure;
    UIO<A> uio = UIOOf.narrowK(value);
    return uio.redeemWith(mapError, map);
  }
}

interface UIOMonadThrow extends UIOMonadError, MonadThrow<UIO_> {

  UIOMonadThrow INSTANCE = new UIOMonadThrow() {};
}

interface UIODefer extends Defer<UIO_> {

  @Override
  default <A> UIO<A>
          defer(Producer<? extends Kind<UIO_, ? extends A>> defer) {
    return UIO.defer(defer::get);
  }
}

interface UIOBracket extends UIOMonadError, Bracket<UIO_, Throwable> {

  @Override
  default <A, B> UIO<B>
          bracket(Kind<UIO_, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<UIO_, ? extends B>> use,
                  Function1<? super A, ? extends Kind<UIO_, Unit>> release) {
    return UIO.bracket(acquire, use, release);
  }
}

interface UIOMonadDefer
    extends MonadDefer<UIO_>, UIODefer, UIOBracket {

  UIOMonadDefer INSTANCE = new UIOMonadDefer() {};

  @Override
  default UIO<Unit> sleep(Duration duration) {
    return UIO.sleep(duration);
  }
}

interface UIOAsync extends Async<UIO_>, UIOMonadDefer {

  UIOAsync INSTANCE = new UIOAsync() {};
  
  @Override
  default <A> UIO<A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<UIO_, Unit>> consumer) {
    return UIO.cancellable(consumer.andThen(UIOOf::narrowK));
  }
}

interface UIOConcurrent extends Concurrent<UIO_>, UIOAsync {
  
  static UIOConcurrent instance(Executor executor) {
    return () -> executor;
  }
  
  Executor executor();
  
  @Override
  default <A, B> UIO<Either<Tuple2<A, Fiber<UIO_, B>>, Tuple2<Fiber<UIO_, A>, B>>> racePair(Kind<UIO_, ? extends A> fa, Kind<UIO_, ? extends B> fb) {
    return UIO.racePair(executor(), fa, fb);
  }
  
  @Override
  default <A> UIO<Fiber<UIO_, A>> fork(Kind<UIO_, ? extends A> value) {
    UIO<A> fix = value.fix(UIOOf::narrowK);
    return fix.fork();
  }
}

final class UIOConsole implements Console<UIO_> {

  public static final UIOConsole INSTANCE = new UIOConsole();

  private final SystemConsole console = new SystemConsole();

  @Override
  public UIO<String> readln() {
    return UIO.task(console::readln);
  }

  @Override
  public UIO<Unit> println(String text) {
    return UIO.exec(() -> console.println(text));
  }
}

interface UIORuntime extends Runtime<UIO_> {
  
  UIORuntime INSTANCE = new UIORuntime() {};

  @Override
  default <T> T run(Kind<UIO_, T> value) {
    return value.fix(toUIO()).unsafeRunSync();
  }
  
  @Override
  default <T> Sequence<T> run(Sequence<Kind<UIO_, T>> values) {
    return run(UIO.traverse(values.map(UIOOf::<T>narrowK)));
  }

  @Override
  default <T> Future<T> parRun(Kind<UIO_, T> value, Executor executor) {
    return value.fix(toUIO()).runAsync();
  }
  
  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<UIO_, T>> values, Executor executor) {
    return parRun(UIO.traverse(values.map(UIOOf::<T>narrowK)), executor);
  }
}