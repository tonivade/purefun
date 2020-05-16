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
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.UIO_;

import java.time.Duration;

public interface UIOInstances {

  static Functor<UIO_> functor() {
    return UIOFunctor.instance();
  }

  static Applicative<UIO_> applicative() {
    return UIOApplicative.instance();
  }

  static Monad<UIO_> monad() {
    return UIOMonad.instance();
  }

  static MonadError<UIO_, Throwable> monadError() {
    return UIOMonadError.instance();
  }

  static MonadThrow<UIO_> monadThrow() {
    return UIOMonadThrow.instance();
  }

  static MonadDefer<UIO_> monadDefer() {
    return UIOMonadDefer.instance();
  }

  static <A> Reference<UIO_, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
}

@Instance
interface UIOFunctor extends Functor<UIO_> {

  @Override
  default <A, B> Higher1<UIO_, B> map(Higher1<UIO_, A> value, Function1<A, B> map) {
    return UIO_.narrowK(value).map(map).kind1();
  }
}

interface UIOPure extends Applicative<UIO_> {

  @Override
  default <A> Higher1<UIO_, A> pure(A value) {
    return UIO.pure(value).kind1();
  }
}

@Instance
interface UIOApplicative extends UIOPure {

  @Override
  default <A, B> Higher1<UIO_, B> ap(Higher1<UIO_, A> value, Higher1<UIO_, Function1<A, B>> apply) {
    return UIO_.narrowK(apply).flatMap(map -> UIO_.narrowK(value).map(map)).kind1();
  }
}

@Instance
interface UIOMonad extends UIOPure, Monad<UIO_> {

  @Override
  default <A, B> Higher1<UIO_, B> flatMap(Higher1<UIO_, A> value, Function1<A, ? extends Higher1<UIO_, B>> map) {
    return UIO_.narrowK(value).flatMap(map.andThen(UIO_::narrowK)).kind1();
  }
}

@Instance
interface UIOMonadError extends UIOMonad, MonadError<UIO_, Throwable> {

  @Override
  default <A> Higher1<UIO_, A> raiseError(Throwable error) {
    return UIO.<A>raiseError(error).kind1();
  }

  @Override
  default <A> Higher1<UIO_, A>
          handleErrorWith(Higher1<UIO_, A> value,
                          Function1<Throwable, ? extends Higher1<UIO_, A>> handler) {
    Function1<Throwable, UIO<A>> mapError = handler.andThen(UIO_::narrowK);
    Function1<A, UIO<A>> map = UIO::pure;
    UIO<A> uio = UIO_.narrowK(value);
    return uio.redeemWith(mapError, map).kind1();
  }
}

@Instance
interface UIOMonadThrow
    extends UIOMonadError,
            MonadThrow<UIO_> { }

interface UIODefer extends Defer<UIO_> {

  @Override
  default <A> Higher1<UIO_, A>
          defer(Producer<Higher1<UIO_, A>> defer) {
    return UIO.defer(() -> defer.map(UIO_::narrowK).get()).kind1();
  }
}

interface UIOBracket extends Bracket<UIO_> {

  @Override
  default <A, B> Higher1<UIO_, B>
          bracket(Higher1<UIO_, A> acquire,
                  Function1<A, ? extends Higher1<UIO_, B>> use,
                  Consumer1<A> release) {
    return UIO.bracket(acquire.fix1(UIO_::narrowK), use.andThen(UIO_::narrowK), release).kind1();
  }
}

@Instance
interface UIOMonadDefer
    extends MonadDefer<UIO_>, UIOMonadThrow, UIODefer, UIOBracket {
  @Override
  default Higher1<UIO_, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).kind1();
  }
}
