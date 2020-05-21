/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.concurrent.Future_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
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

  static MonadDefer<Future_> monadDefer() {
    return monadDefer(Future.DEFAULT_EXECUTOR);
  }

  static MonadDefer<Future_> monadDefer(Executor executor) {
    return FutureMonadDefer.instance(checkNonNull(executor));
  }
}

interface FutureFunctor extends Functor<Future_> {

  FutureFunctor INSTANCE = new FutureFunctor() {};

  @Override
  default <T, R> Kind<Future_, R> map(Kind<Future_, T> value, Function1<T, R> mapper) {
    return FutureOf.narrowK(value).map(mapper);
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
  default <T, R> Kind<Future_, R> ap(Kind<Future_, T> value, Kind<Future_, Function1<T, R>> apply) {
    return FutureOf.narrowK(value).flatMap(t -> FutureOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface FutureMonad extends FuturePure, Monad<Future_> {

  static FutureMonad instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Kind<Future_, R> flatMap(Kind<Future_, T> value,
      Function1<T, ? extends Kind<Future_, R>> map) {
    return FutureOf.narrowK(value).flatMap(map.andThen(FutureOf::narrowK));
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
  default <A> Kind<Future_, A> handleErrorWith(Kind<Future_, A> value,
      Function1<Throwable, ? extends Kind<Future_, A>> handler) {
    return FutureOf.narrowK(value).fold(handler.andThen(FutureOf::narrowK),
                                      success -> Future.success(executor(), success)).flatMap(identity());
  }
}

interface FutureDefer extends Defer<Future_>, ExecutorHolder {

  @Override
  default <A> Kind<Future_, A> defer(Producer<Kind<Future_, A>> defer) {
    return Future.defer(executor(), defer.map(FutureOf::narrowK)::get);
  }
}

interface FutureBracket extends Bracket<Future_>, ExecutorHolder {

  @Override
  default <A, B> Kind<Future_, B> bracket(Kind<Future_, A> acquire, Function1<A, ? extends Kind<Future_, B>> use, Consumer1<A> release) {
    return Future.bracket(executor(), FutureOf.narrowK(acquire), use.andThen(FutureOf::narrowK), release);
  }
}

interface FutureMonadDefer extends MonadDefer<Future_>, FutureMonadThrow, FutureDefer, FutureBracket {

  static FutureMonadDefer instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default Kind<Future_, Unit> sleep(Duration duration) {
    return Future.sleep(executor(), duration);
  }
}
