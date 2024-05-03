/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.effect.EIOOf.toEIO;
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
import com.github.tonivade.purefun.effect.EIO;
import com.github.tonivade.purefun.effect.EIOOf;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Async;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Concurrent;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Runtime;

@SuppressWarnings("unchecked")
public interface EIOInstances {

  static <E> Functor<Kind<EIO<?, ?>, E>> functor() {
    return EIOFunctor.INSTANCE;
  }

  static <E> Applicative<Kind<EIO<?, ?>, E>> applicative() {
    return EIOApplicative.INSTANCE;
  }

  static <E> Monad<Kind<EIO<?, ?>, E>> monad() {
    return EIOMonad.INSTANCE;
  }

  static <E> MonadError<Kind<EIO<?, ?>, E>, E> monadError() {
    return EIOMonadError.INSTANCE;
  }

  static MonadThrow<Kind<EIO<?, ?>, Throwable>> monadThrow() {
    return EIOMonadThrow.INSTANCE;
  }

  static MonadDefer<Kind<EIO<?, ?>, Throwable>> monadDefer() {
    return EIOMonadDefer.INSTANCE;
  }

  static Async<Kind<EIO<?, ?>, Throwable>> async() {
    return EIOAsync.INSTANCE;
  }

  static Concurrent<Kind<EIO<?, ?>, Throwable>> concurrent() {
    return concurrent(Future.DEFAULT_EXECUTOR);
  }

  static Concurrent<Kind<EIO<?, ?>, Throwable>> concurrent(Executor executor) {
    return EIOConcurrent.instance(executor);
  }

  static <E> Runtime<Kind<EIO<?, ?>, E>> runtime() {
    return EIORuntime.INSTANCE;
  }
}

interface EIOFunctor<E> extends Functor<Kind<EIO<?, ?>, E>> {

  @SuppressWarnings("rawtypes")
  EIOFunctor INSTANCE = new EIOFunctor() {};

  @Override
  default <A, B> EIO<E, B>
          map(Kind<Kind<EIO<?, ?>, E>, ? extends A> value, Function1<? super A, ? extends B> map) {
    return EIOOf.narrowK(value).map(map);
  }
}

interface EIOPure<E> extends Applicative<Kind<EIO<?, ?>, E>> {

  @Override
  default <A> EIO<E, A> pure(A value) {
    return EIO.pure(value);
  }
}

interface EIOApplicative<E> extends EIOPure<E> {

  @SuppressWarnings("rawtypes")
  EIOApplicative INSTANCE = new EIOApplicative() {};

  @Override
  default <A, B> EIO<E, B>
          ap(Kind<Kind<EIO<?, ?>, E>, ? extends A> value,
             Kind<Kind<EIO<?, ?>, E>, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(EIOOf::<E, A>narrowK).ap(apply.fix(EIOOf::narrowK));
  }
}

interface EIOMonad<E> extends EIOPure<E>, Monad<Kind<EIO<?, ?>, E>> {

  @SuppressWarnings("rawtypes")
  EIOMonad INSTANCE = new EIOMonad() {};

  @Override
  default <A, B> EIO<E, B>
          flatMap(Kind<Kind<EIO<?, ?>, E>, ? extends A> value,
                  Function1<? super A, ? extends Kind<Kind<EIO<?, ?>, E>, ? extends B>> map) {
    return value.fix(toEIO()).flatMap(map.andThen(EIOOf::narrowK));
  }
}

interface EIOMonadError<E> extends EIOMonad<E>, MonadError<Kind<EIO<?, ?>, E>, E> {

  @SuppressWarnings("rawtypes")
  EIOMonadError INSTANCE = new EIOMonadError() {};

  @Override
  default <A> EIO<E, A> raiseError(E error) {
    return EIO.raiseError(error);
  }

  @Override
  default <A> EIO<E, A> handleErrorWith(
      Kind<Kind<EIO<?, ?>,  E>, A> value,
      Function1<? super E, ? extends Kind<Kind<EIO<?, ?>, E>, ? extends A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<? super E, EIO<E, A>> mapError = handler.andThen(EIOOf::narrowK);
    Function1<A, EIO<E, A>> map = EIO::pure;
    EIO<E, A> eio = EIOOf.narrowK(value);
    return eio.foldM(mapError, map);
  }
}

interface EIOMonadThrow
    extends EIOMonadError<Throwable>,
            MonadThrow<Kind<EIO<?, ?>, Throwable>> {

  EIOMonadThrow INSTANCE = new EIOMonadThrow() {};
}

interface EIODefer<E> extends Defer<Kind<EIO<?, ?>, E>> {

  @Override
  default <A> EIO<E, A>
          defer(Producer<? extends Kind<Kind<EIO<?, ?>, E>, ? extends A>> defer) {
    return EIO.defer(defer::get);
  }
}

interface EIOBracket<E> extends EIOMonadError<E>, Bracket<Kind<EIO<?, ?>, E>, E> {

  @Override
  default <A, B> EIO<E, B>
          bracket(Kind<Kind<EIO<?, ?>, E>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<Kind<EIO<?, ?>, E>, ? extends B>> use,
                  Function1<? super A, ? extends Kind<Kind<EIO<?, ?>, E>, Unit>> release) {
    return EIO.bracket(acquire, use, release);
  }
}

interface EIOMonadDefer
    extends MonadDefer<Kind<EIO<?, ?>, Throwable>>, EIODefer<Throwable>, EIOBracket<Throwable> {

  EIOMonadDefer INSTANCE = new EIOMonadDefer() {};

  @Override
  default EIO<Throwable, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).toEIO();
  }
}

interface EIOAsync extends Async<Kind<EIO<?, ?>, Throwable>>, EIOMonadDefer {

  EIOAsync INSTANCE = new EIOAsync() {};

  @Override
  default <A> EIO<Throwable, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<Kind<EIO<?, ?>, Throwable>, Unit>> consumer) {
    return EIO.cancellable(cb -> consumer.andThen(EIOOf::narrowK).apply(e -> cb.accept(Try.success(e.toEither()))));
  }
}

interface EIOConcurrent extends EIOAsync, Concurrent<Kind<EIO<?, ?>, Throwable>> {

  static EIOConcurrent instance(Executor executor) {
    return () -> executor;
  }

  Executor executor();

  @Override
  default <A, B> EIO<Throwable, Either<Tuple2<A, Fiber<Kind<EIO<?, ?>, Throwable>, B>>, Tuple2<Fiber<Kind<EIO<?, ?>, Throwable>, A>, B>>> racePair(
    Kind<Kind<EIO<?, ?>, Throwable>, ? extends A> fa, Kind<Kind<EIO<?, ?>, Throwable>, ? extends B> fb) {
    return EIO.racePair(executor(), fa, fb);
  }

  @Override
  default <A> EIO<Throwable, Fiber<Kind<EIO<?, ?>, Throwable>, A>> fork(Kind<Kind<EIO<?, ?>, Throwable>, ? extends A> value) {
    EIO<Throwable, A> fix = value.fix(EIOOf::narrowK);
    return fix.fork();
  }
}

interface EIORuntime<E> extends Runtime<Kind<EIO<?, ?>, E>> {

  @SuppressWarnings("rawtypes")
  EIORuntime INSTANCE = new EIORuntime() {};

  @Override
  default <T> T run(Kind<Kind<EIO<?, ?>, E>, T> value) {
    return value.fix(toEIO()).safeRunSync().getRight();
  }

  @Override
  default <T> Sequence<T> run(Sequence<Kind<Kind<EIO<?, ?>, E>, T>> values) {
    return run(EIO.traverse(values.map(EIOOf::<E, T>narrowK)));
  }

  @Override
  default <T> Future<T> parRun(Kind<Kind<EIO<?, ?>, E>, T> value, Executor executor) {
    return value.fix(toEIO()).runAsync().map(Either::get);
  }

  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<Kind<EIO<?, ?>, E>, T>> values, Executor executor) {
    return parRun(EIO.traverse(values.map(EIOOf::<E, T>narrowK)), executor);
  }
}