/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;

import java.util.concurrent.Executor;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

public interface FutureInstances {

  static Functor<Future.µ> functor() {
    return new FutureFunctor() {};
  }

  static Applicative<Future.µ> applicative() {
    return new FutureApplicative() { };
  }

  static Monad<Future.µ> monad() {
    return new FutureMonad() { };
  }

  static MonadError<Future.µ, Throwable> monadError() {
    return new FutureMonadThrow() { };
  }

  static MonadDefer<Future.µ> monadDefer() {
    return new FutureMonadDefer() { };
  }
}

@Instance
interface FutureFunctor extends Functor<Future.µ> {

  @Override
  default <T, R> Higher1<Future.µ, R> map(Higher1<Future.µ, T> value, Function1<T, R> mapper) {
    return Future.narrowK(value).map(mapper).kind1();
  }
}

@Instance
interface FuturePure extends Applicative<Future.µ> {

  @Override
  default <T> Higher1<Future.µ, T> pure(T value) {
    return Future.success(value).kind1();
  }
}

@Instance
interface FutureApplicative extends FuturePure {

  @Override
  default <T, R> Higher1<Future.µ, R> ap(Higher1<Future.µ, T> value, Higher1<Future.µ, Function1<T, R>> apply) {
    return Future.narrowK(value).flatMap(t -> Future.narrowK(apply).map(f -> f.apply(t))).kind1();
  }
}

@Instance
interface FutureMonad extends FuturePure, Monad<Future.µ> {

  @Override
  default <T, R> Higher1<Future.µ, R> flatMap(Higher1<Future.µ, T> value,
      Function1<T, ? extends Higher1<Future.µ, R>> map) {
    return Future.narrowK(value).flatMap(map.andThen(Future::narrowK)).kind1();
  }
}

@Instance
interface FutureMonadThrow extends FutureMonad, MonadThrow<Future.µ> {

  @Override
  default <A> Higher1<Future.µ, A> raiseError(Throwable error) {
    return Future.<A>failure(error).kind1();
  }

  @Override
  default <A> Higher1<Future.µ, A> handleErrorWith(Higher1<Future.µ, A> value,
      Function1<Throwable, ? extends Higher1<Future.µ, A>> handler) {
    return Future.narrowK(value).fold(handler.andThen(Future::narrowK),
                                      success -> Future.success(success)).flatMap(identity()).kind1();
  }
}

@Instance
interface FutureDefer extends Defer<Future.µ> {

  @Override
  default <A> Higher1<Future.µ, A> defer(Producer<Higher1<Future.µ, A>> defer) {
    return Future.defer(defer.map(Future::narrowK)::get).kind1();
  }
}

@Instance
interface FutureBracket extends Bracket<Future.µ> {

  @Override
  default <A, B> Higher1<Future.µ, B> bracket(Higher1<Future.µ, A> acquire, Function1<A, ? extends Higher1<Future.µ, B>> use, Consumer1<A> release) {
    // TODO:
//    return Future.narrowK(acquire)
//      .flatMap(resource -> use.andThen(Future::narrowK).apply(resource)
//          .onComplete(result -> release.accept(resource))).kind1();
    return null;
  }
}

@Instance
interface FutureMonadDefer extends MonadDefer<Future.µ>, FutureMonadThrow, FutureDefer, FutureBracket {

}
