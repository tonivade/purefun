/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.monad.Future;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;

public interface FutureInstances {

  static Functor<Future.µ> functor() {
    return new FutureFunctor() {};
  }

  static Applicative<Future.µ> applicative() {
    return new FutureApplicative() {};
  }

  static Monad<Future.µ> monad() {
    return new FutureMonad() {};
  }

  static MonadError<Future.µ, Throwable> monadError() {
    return new FutureMonadError() {};
  }
}

interface FutureFunctor extends Functor<Future.µ> {

  @Override
  default <T, R> Future<R> map(Higher1<Future.µ, T> value, Function1<T, R> mapper) {
    return Future.narrowK(value).map(mapper);
  }
}

interface FuturePure extends Applicative<Future.µ> {

  @Override
  default <T> Future<T> pure(T value) {
    return Future.success(value);
  }
}

interface FutureApplicative extends FuturePure {

  @Override
  default <T, R> Future<R> ap(Higher1<Future.µ, T> value, Higher1<Future.µ, Function1<T, R>> apply) {
    return Future.narrowK(value).flatMap(t -> Future.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface FutureMonad extends FuturePure, Monad<Future.µ> {

  @Override
  default <T, R> Future<R> flatMap(Higher1<Future.µ, T> value,
      Function1<T, ? extends Higher1<Future.µ, R>> map) {
    return Future.narrowK(value).flatMap(map);
  }
}

interface FutureMonadError extends FutureMonad, MonadError<Future.µ, Throwable> {

  @Override
  default <A> Future<A> raiseError(Throwable error) {
    return Future.failure(error);
  }

  @Override
  default <A> Future<A> handleErrorWith(Higher1<Future.µ, A> value,
      Function1<Throwable, ? extends Higher1<Future.µ, A>> handler) {
    return Future.narrowK(value).fold(handler.andThen(Future::narrowK), Future::success).flatten();
  }
}
