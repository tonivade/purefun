/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.concurrent.FutureOf.toFuture;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.concurrent.Future_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;

public interface FutureInstances {

  static Functor<Future_> functor() {
    return FutureFunctor.INSTANCE;
  }

  static Applicative<Future_> applicative() {
    return applicative(Future.DEFAULT_EXECUTOR);
  }

  static Applicative<Future_> applicative(Executor executor) {
    return FutureApplicative.instance(checkNonNull(executor));
  }

  static Monad<Future_> monad() {
    return monad(Future.DEFAULT_EXECUTOR);
  }

  static Monad<Future_> monad(Executor executor) {
    return FutureMonad.instance(checkNonNull(executor));
  }

  static MonadError<Future_, Throwable> monadError() {
    return monadError(Future.DEFAULT_EXECUTOR);
  }

  static MonadError<Future_, Throwable> monadError(Executor executor) {
    return FutureMonadThrow.instance(checkNonNull(executor));
  }
}

interface FutureFunctor extends Functor<Future_> {

  FutureFunctor INSTANCE = new FutureFunctor() {};

  @Override
  default <T, R> Kind<Future_, R> map(Kind<Future_, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return value.fix(toFuture()).map(mapper);
  }
}

interface ExecutorHolder {
  Executor executor();
}

interface FuturePure extends Applicative<Future_>, ExecutorHolder {

  @Override
  default <T> Kind<Future_, T> pure(T value) {
    return Future.success(executor(), value);
  }
}

interface FutureApplicative extends FuturePure {

  static FutureApplicative instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Kind<Future_, R> ap(Kind<Future_, ? extends T> value, 
      Kind<Future_, ? extends Function1<? super T, ? extends R>> apply) {
    return value.fix(FutureOf::<T>narrowK).ap(apply.fix(FutureOf::narrowK));
  }
}

interface FutureMonad extends FuturePure, Monad<Future_> {

  static FutureMonad instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Kind<Future_, R> flatMap(Kind<Future_, ? extends T> value,
      Function1<? super T, ? extends Kind<Future_, ? extends R>> map) {
    return value.fix(toFuture()).flatMap(map.andThen(FutureOf::narrowK));
  }

  /**
   * XXX In order to create real parallel computations, we need to override ap to use the
   * applicative version of the ap method
   */
  @Override
  default <T, R> Kind<Future_, R> ap(Kind<Future_, ? extends T> value, 
      Kind<Future_, ? extends Function1<? super T, ? extends R>> apply) {
    return FutureInstances.applicative(executor()).ap(value, apply);
  }
}

interface FutureMonadThrow extends FutureMonad, MonadThrow<Future_> {

  static FutureMonadThrow instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <A> Kind<Future_, A> raiseError(Throwable error) {
    return Future.<A>failure(executor(), error);
  }

  @Override
  default <A> Kind<Future_, A> handleErrorWith(
      Kind<Future_, A> value,
      Function1<? super Throwable, ? extends Kind<Future_, ? extends A>> handler) {
    return value.fix(toFuture()).fold(handler.andThen(FutureOf::narrowK),
                                      success -> Future.success(executor(), success)).flatMap(identity());
  }
}
