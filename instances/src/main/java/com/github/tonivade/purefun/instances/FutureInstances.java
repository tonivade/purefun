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
import com.github.tonivade.purefun.concurrent.Future_;
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
import static com.github.tonivade.purefun.Precondition.checkNonNull;

public interface FutureInstances {

  static Functor<Future_> functor() {
    return FutureFunctor.instance();
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

@Instance
interface FutureFunctor extends Functor<Future_> {

  @Override
  default <T, R> Higher1<Future_, R> map(Higher1<Future_, T> value, Function1<T, R> mapper) {
    return Future_.narrowK(value).map(mapper).kind1();
  }
}

interface ExecutorHolder {
  Executor executor();
}

interface FuturePure extends Applicative<Future_>, ExecutorHolder {

  @Override
  default <T> Higher1<Future_, T> pure(T value) {
    return Future.success(executor(), value).kind1();
  }
}

interface FutureApplicative extends FuturePure {

  static FutureApplicative instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Higher1<Future_, R> ap(Higher1<Future_, T> value, Higher1<Future_, Function1<T, R>> apply) {
    return Future_.narrowK(value).flatMap(t -> Future_.narrowK(apply).map(f -> f.apply(t))).kind1();
  }
}

interface FutureMonad extends FuturePure, Monad<Future_> {

  static FutureMonad instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Higher1<Future_, R> flatMap(Higher1<Future_, T> value,
      Function1<T, ? extends Higher1<Future_, R>> map) {
    return Future_.narrowK(value).flatMap(map.andThen(Future_::narrowK)).kind1();
  }
}

interface FutureMonadThrow extends FutureMonad, MonadThrow<Future_> {

  static FutureMonadThrow instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <A> Higher1<Future_, A> raiseError(Throwable error) {
    return Future.<A>failure(executor(), error).kind1();
  }

  @Override
  default <A> Higher1<Future_, A> handleErrorWith(Higher1<Future_, A> value,
      Function1<Throwable, ? extends Higher1<Future_, A>> handler) {
    return Future_.narrowK(value).fold(handler.andThen(Future_::narrowK),
                                      success -> Future.success(executor(), success)).flatMap(identity()).kind1();
  }
}

interface FutureDefer extends Defer<Future_>, ExecutorHolder {

  @Override
  default <A> Higher1<Future_, A> defer(Producer<Higher1<Future_, A>> defer) {
    return Future.defer(executor(), defer.map(Future_::narrowK)::get).kind1();
  }
}

interface FutureBracket extends Bracket<Future_>, ExecutorHolder {

  @Override
  default <A, B> Higher1<Future_, B> bracket(Higher1<Future_, A> acquire, Function1<A, ? extends Higher1<Future_, B>> use, Consumer1<A> release) {
    return Future.bracket(executor(), Future_.narrowK(acquire), use.andThen(Future_::narrowK), release).kind1();
  }
}

interface FutureMonadDefer extends MonadDefer<Future_>, FutureMonadThrow, FutureDefer, FutureBracket {

  static FutureMonadDefer instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default Higher1<Future_, Unit> sleep(Duration duration) {
    return Future.sleep(executor(), duration).kind1();
  }
}
