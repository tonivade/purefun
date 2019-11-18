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
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.zio.UIO;

public interface UIOInstances {

  static <E> Functor<UIO.µ> functor() {
    return new UIOFunctor() {};
  }

  static <E> Applicative<UIO.µ> applicative() {
    return new UIOApplicative() {};
  }

  static <E> Monad<UIO.µ> monad() {
    return new UIOMonad() {};
  }

  static <E> MonadError<UIO.µ, Throwable> monadError() {
    return new UIOMonadError() {};
  }

  static MonadThrow<UIO.µ> monadThrow() {
    return new UIOMonadThrow() { };
  }

  static MonadDefer<UIO.µ> monadDefer() {
    return new UIOMonadDefer() { };
  }

  static <A> Reference<UIO.µ, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
}

@Instance
interface UIOFunctor extends Functor<UIO.µ> {

  @Override
  default <A, B> Higher1<UIO.µ, B> map(Higher1<UIO.µ, A> value, Function1<A, B> map) {
    return UIO.narrowK(value).map(map).kind1();
  }
}

@Instance
interface UIOPure extends Applicative<UIO.µ> {

  @Override
  default <A> Higher1<UIO.µ, A> pure(A value) {
    return UIO.pure(value).kind1();
  }
}

@Instance
interface UIOApplicative extends UIOPure {

  @Override
  default <A, B> Higher1<UIO.µ, B> ap(Higher1<UIO.µ, A> value, Higher1<UIO.µ, Function1<A, B>> apply) {
    return UIO.narrowK(apply).flatMap(map -> UIO.narrowK(value).map(map)).kind1();
  }
}

@Instance
interface UIOMonad extends UIOPure, Monad<UIO.µ> {

  @Override
  default <A, B> Higher1<UIO.µ, B> flatMap(Higher1<UIO.µ, A> value, Function1<A, ? extends Higher1<UIO.µ, B>> map) {
    return UIO.narrowK(value).flatMap(map.andThen(UIO::narrowK)).kind1();
  }
}

@Instance
interface UIOMonadError extends UIOMonad, MonadError<UIO.µ, Throwable> {

  @Override
  default <A> Higher1<UIO.µ, A> raiseError(Throwable error) {
    return UIO.<A>raiseError(error).kind1();
  }

  @Override
  default <A> Higher1<UIO.µ, A>
          handleErrorWith(Higher1<UIO.µ, A> value,
                          Function1<Throwable, ? extends Higher1<UIO.µ, A>> handler) {
    Function1<Throwable, UIO<A>> mapError = handler.andThen(UIO::narrowK);
    Function1<A, UIO<A>> map = UIO::pure;
    UIO<A> uio = UIO.narrowK(value);
    return uio.redeemWith(mapError, map).kind1();
  }
}

@Instance
interface UIOMonadThrow
    extends UIOMonadError,
            MonadThrow<UIO.µ> { }

@Instance
interface UIODefer extends Defer<UIO.µ> {

  @Override
  default <A> Higher1<UIO.µ, A>
          defer(Producer<Higher1<UIO.µ, A>> defer) {
    return UIO.defer(() -> defer.map(UIO::narrowK).get()).kind1();
  }
}

@Instance
interface UIOBracket extends Bracket<UIO.µ> {

  @Override
  default <A, B> Higher1<UIO.µ, B>
          bracket(Higher1<UIO.µ, A> acquire,
                  Function1<A, ? extends Higher1<UIO.µ, B>> use,
                  Consumer1<A> release) {
    return UIO.bracket(acquire.fix1(UIO::narrowK), use.andThen(UIO::narrowK), release).kind1();
  }
}

@Instance
interface UIOMonadDefer
    extends MonadDefer<UIO.µ>,
            UIOMonadThrow,
            UIODefer,
            UIOBracket { }
