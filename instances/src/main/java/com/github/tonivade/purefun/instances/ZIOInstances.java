/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.zio.ZIO;

public interface ZIOInstances {

  static <R, E> Functor<Higher1<Higher1<ZIO.µ, R>, E>> functor() {
    return new ZIOFunctor<R, E>() {};
  }

  static <R, E> Applicative<Higher1<Higher1<ZIO.µ, R>, E>> applicative() {
    return new ZIOApplicative<R, E>() {};
  }

  static <R, E> Monad<Higher1<Higher1<ZIO.µ, R>, E>> monad() {
    return new ZIOMonad<R, E>() {};
  }

  static <R, E> MonadError<Higher1<Higher1<ZIO.µ, R>, E>, E> monadError() {
    return new ZIOMonadError<R, E>() {};
  }

  static <R> MonadThrow<Higher1<Higher1<ZIO.µ, R>, Throwable>> monadThrow() {
    return new ZIOMonadThrow<R>() { };
  }

  static <R> MonadDefer<Higher1<Higher1<ZIO.µ, R>, Throwable>> monadDefer() {
    return new ZIOMonadDefer<R>() { };
  }

  static <R, A> Reference<Higher1<Higher1<ZIO.µ, R>, Throwable>, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
}

interface ZIOFunctor<R, E> extends Functor<Higher1<Higher1<ZIO.µ, R>, E>> {

  @Override
  default <A, B> ZIO<R, E, B>
          map(Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A> value, Function1<A, B> map) {
    return ZIO.narrowK(value).map(map);
  }
}

interface ZIOPure<R, E> extends Applicative<Higher1<Higher1<ZIO.µ, R>, E>> {

  @Override
  default <A> ZIO<R, E, A> pure(A value) {
    return ZIO.pure(value);
  }
}

interface ZIOApplicative<R, E> extends ZIOPure<R, E> {

  @Override
  default <A, B> ZIO<R, E, B>
          ap(Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A> value,
             Higher1<Higher1<Higher1<ZIO.µ, R>, E>, Function1<A, B>> apply) {
    return ZIO.narrowK(apply).flatMap(map -> ZIO.narrowK(value).map(map));
  }
}

interface ZIOMonad<R, E> extends ZIOPure<R, E>, Monad<Higher1<Higher1<ZIO.µ, R>, E>> {

  @Override
  default <A, B> ZIO<R, E, B>
          flatMap(Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A> value,
                  Function1<A, ? extends Higher1<Higher1<Higher1<ZIO.µ, R>, E>, B>> map) {
    return ZIO.narrowK(value).flatMap(map.andThen(ZIO::narrowK));
  }
}

interface ZIOMonadError<R, E> extends ZIOMonad<R, E>, MonadError<Higher1<Higher1<ZIO.µ, R>, E>, E> {

  @Override
  default <A> ZIO<R, E, A> raiseError(E error) {
    return ZIO.raiseError(error);
  }

  @Override
  default <A> ZIO<R, E, A>
          handleErrorWith(Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A> value,
                          Function1<E, ? extends Higher1<Higher1<Higher1<ZIO.µ, R>, E>, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<E, ZIO<R, E, A>> mapError = handler.andThen(ZIO::narrowK);
    Function1<A, ZIO<R, E, A>> map = ZIO::pure;
    ZIO<R, E, A> zio = ZIO.narrowK(value);
    return zio.foldM(mapError, map);
  }
}

interface ZIOMonadThrow<R>
    extends ZIOMonadError<R, Throwable>,
            MonadThrow<Higher1<Higher1<ZIO.µ, R>, Throwable>> { }

interface ZIODefer<R> extends Defer<Higher1<Higher1<ZIO.µ, R>, Throwable>> {

  @Override
  default <A> ZIO<R, Throwable, A>
          defer(Producer<Higher1<Higher1<Higher1<ZIO.µ, R>, Throwable>, A>> defer) {
    return ZIO.defer(() -> defer.andThen(ZIO::narrowK).get());
  }
}

interface ZIOBracket<R> extends Bracket<Higher1<Higher1<ZIO.µ, R>, Throwable>> {

  @Override
  default <A, B> ZIO<R, Throwable, B>
          bracket(Higher1<Higher1<Higher1<ZIO.µ, R>, Throwable>, A> acquire,
                  Function1<A, ? extends Higher1<Higher1<Higher1<ZIO.µ, R>, Throwable>, B>> use,
                  Consumer1<A> release) {
    return ZIO.bracket(acquire.fix1(ZIO::narrowK), use.andThen(ZIO::narrowK), release);
  }
}

interface ZIOMonadDefer<R>
    extends MonadDefer<Higher1<Higher1<ZIO.µ, R>, Throwable>>,
            ZIOMonadThrow<R>,
            ZIODefer<R>,
            ZIOBracket<R> { }
