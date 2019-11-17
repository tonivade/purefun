/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
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
import com.github.tonivade.purefun.zio.EIO;
import com.github.tonivade.purefun.zio.UIO;

public interface UIOInstances {

  static <E> Functor<Higher1<EIO.µ, E>> functor() {
    return new EIOFunctor<E>() {};
  }

  static <E> Applicative<Higher1<EIO.µ, E>> applicative() {
    return new EIOApplicative<E>() {};
  }

  static <E> Monad<Higher1<EIO.µ, E>> monad() {
    return new EIOMonad<E>() {};
  }

  static <E> MonadError<Higher1<EIO.µ, E>, E> monadError() {
    return new EIOMonadError<E>() {};
  }

  static MonadThrow<Higher1<EIO.µ, Throwable>> monadThrow() {
    return new EIOMonadThrow() { };
  }

  static MonadDefer<Higher1<EIO.µ, Throwable>> monadDefer() {
    return new EIOMonadDefer() { };
  }

  static <A> Reference<Higher1<EIO.µ, Throwable>, A> ref(A value) {
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
    return UIO.<A>pure(value).kind1();
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
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<Throwable, UIO<A>> mapError = handler.andThen(UIO::narrowK);
    Function1<A, UIO<A>> map = UIO::pure;
    UIO<A> uio = UIO.narrowK(value);
    // TODO: foldM
    // return uio.foldM(mapError, map).kind2();
    return null;
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
    // TODO: bracket
    // return UIO.bracket(acquire.fix1(UIO::narrowK), use.andThen(UIO::narrowK), release).kind1();
    return null;
  }
}

@Instance
interface UIOMonadDefer
    extends MonadDefer<UIO.µ>,
            UIOMonadThrow,
            UIODefer,
            UIOBracket { }
