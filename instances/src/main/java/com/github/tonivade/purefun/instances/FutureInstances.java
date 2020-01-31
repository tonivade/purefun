/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;

import java.time.Duration;
import java.util.concurrent.Executor;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

public interface FutureInstances {

  static Functor<Future.µ> functor() {
    return FutureFunctor.instance();
  }

  static Applicative<Future.µ> applicative() {
    return applicative(Future.DEFAULT_EXECUTOR);
  }

  static Applicative<Future.µ> applicative(Executor executor) {
    return FutureApplicative.instance(requireNonNull(executor));
  }

  static Monad<Future.µ> monad() {
    return monad(Future.DEFAULT_EXECUTOR);
  }

  static Monad<Future.µ> monad(Executor executor) {
    return FutureMonad.instance(requireNonNull(executor));
  }

  static MonadError<Future.µ, Throwable> monadError() {
    return monadError(Future.DEFAULT_EXECUTOR);
  }

  static MonadError<Future.µ, Throwable> monadError(Executor executor) {
    return FutureMonadThrow.instance(requireNonNull(executor));
  }

  static MonadDefer<Future.µ> monadDefer() {
    return monadDefer(Future.DEFAULT_EXECUTOR);
  }

  static MonadDefer<Future.µ> monadDefer(Executor executor) {
    return FutureMonadDefer.instance(requireNonNull(executor));
  }
}

@Instance
interface FutureFunctor extends Functor<Future.µ> {

  @Override
  default <T, R> Higher1<Future.µ, R> map(Higher1<Future.µ, T> value, Function1<T, R> mapper) {
    return Future.narrowK(value).map(mapper).kind1();
  }
}

interface ExecutorHolder {
  Executor executor();
}

interface FuturePure extends Applicative<Future.µ>, ExecutorHolder {

  @Override
  default <T> Higher1<Future.µ, T> pure(T value) {
    return Future.success(executor(), value).kind1();
  }
}

interface FutureApplicative extends FuturePure {

  static FutureApplicative instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Higher1<Future.µ, R> ap(Higher1<Future.µ, T> value, Higher1<Future.µ, Function1<T, R>> apply) {
    return Future.narrowK(value).flatMap(t -> Future.narrowK(apply).map(f -> f.apply(t))).kind1();
  }
}

interface FutureMonad extends FuturePure, Monad<Future.µ> {

  static FutureMonad instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Higher1<Future.µ, R> flatMap(Higher1<Future.µ, T> value,
      Function1<T, ? extends Higher1<Future.µ, R>> map) {
    return Future.narrowK(value).flatMap(map.andThen(Future::narrowK)).kind1();
  }
}

interface FutureMonadThrow extends FutureMonad, MonadThrow<Future.µ> {

  static FutureMonadThrow instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <A> Higher1<Future.µ, A> raiseError(Throwable error) {
    return Future.<A>failure(executor(), error).kind1();
  }

  @Override
  default <A> Higher1<Future.µ, A> handleErrorWith(Higher1<Future.µ, A> value,
      Function1<Throwable, ? extends Higher1<Future.µ, A>> handler) {
    return Future.narrowK(value).fold(handler.andThen(Future::narrowK),
                                      success -> Future.success(executor(), success)).flatMap(identity()).kind1();
  }
}

interface FutureDefer extends Defer<Future.µ>, ExecutorHolder {

  @Override
  default <A> Higher1<Future.µ, A> defer(Producer<Higher1<Future.µ, A>> defer) {
    return Future.defer(executor(), defer.map(Future::narrowK)::get).kind1();
  }
}

interface FutureBracket extends Bracket<Future.µ>, ExecutorHolder {

  @Override
  default <A, B> Higher1<Future.µ, B> bracket(Higher1<Future.µ, A> acquire, Function1<A, ? extends Higher1<Future.µ, B>> use, Consumer1<A> release) {
    return Future.bracket(executor(), Future.narrowK(acquire), use.andThen(Future::narrowK), release).kind1();
  }
}

interface FutureMonadDefer extends MonadDefer<Future.µ>, FutureMonadThrow, FutureDefer, FutureBracket {

  static FutureMonadDefer instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default Higher1<Future.µ, Unit> sleep(Duration duration) {
    return Future.sleep(executor(), duration).kind1();
  }
}
