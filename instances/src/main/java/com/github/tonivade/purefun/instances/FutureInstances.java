/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;

public interface FutureInstances {

  static Functor<Future<?>> functor() {
    return FutureFunctor.INSTANCE;
  }

  static Applicative<Future<?>> applicative() {
    return applicative(Future.DEFAULT_EXECUTOR);
  }

  static Applicative<Future<?>> applicative(Executor executor) {
    return FutureApplicative.instance(checkNonNull(executor));
  }

  static Monad<Future<?>> monad() {
    return monad(Future.DEFAULT_EXECUTOR);
  }

  static Monad<Future<?>> monad(Executor executor) {
    return FutureMonad.instance(checkNonNull(executor));
  }

  static MonadError<Future<?>, Throwable> monadError() {
    return monadError(Future.DEFAULT_EXECUTOR);
  }

  static MonadError<Future<?>, Throwable> monadError(Executor executor) {
    return FutureMonadThrow.instance(checkNonNull(executor));
  }
}

interface FutureFunctor extends Functor<Future<?>> {

  FutureFunctor INSTANCE = new FutureFunctor() {};

  @Override
  default <T, R> Kind<Future<?>, R> map(Kind<Future<?>, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return value.fix(FutureOf::toFuture).map(mapper);
  }
}

interface ExecutorHolder {
  Executor executor();
}

interface FuturePure extends Applicative<Future<?>>, ExecutorHolder {

  @Override
  default <T> Kind<Future<?>, T> pure(T value) {
    return Future.success(executor(), value);
  }
}

interface FutureApplicative extends FuturePure {

  static FutureApplicative instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Kind<Future<?>, R> ap(Kind<Future<?>, ? extends T> value,
      Kind<Future<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return value.fix(FutureOf::<T>toFuture).ap(apply.fix(FutureOf::toFuture));
  }
}

interface FutureMonad extends FuturePure, Monad<Future<?>> {

  static FutureMonad instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Kind<Future<?>, R> flatMap(Kind<Future<?>, ? extends T> value,
      Function1<? super T, ? extends Kind<Future<?>, ? extends R>> map) {
    return value.fix(FutureOf::toFuture).flatMap(map.andThen(FutureOf::toFuture));
  }

  /**
   * XXX In order to create real parallel computations, we need to override ap to use the
   * applicative version of the ap method
   */
  @Override
  default <T, R> Kind<Future<?>, R> ap(Kind<Future<?>, ? extends T> value,
      Kind<Future<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return FutureInstances.applicative(executor()).ap(value, apply);
  }
}

interface FutureMonadThrow extends FutureMonad, MonadThrow<Future<?>> {

  static FutureMonadThrow instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <A> Kind<Future<?>, A> raiseError(Throwable error) {
    return Future.failure(executor(), error);
  }

  @Override
  default <A> Kind<Future<?>, A> handleErrorWith(
      Kind<Future<?>, A> value,
      Function1<? super Throwable, ? extends Kind<Future<?>, ? extends A>> handler) {
    return value.fix(FutureOf::toFuture).fold(handler.andThen(FutureOf::toFuture),
                                      success -> Future.success(executor(), success)).flatMap(identity());
  }
}
