/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.effect.UIOOf.toUIO;
import java.time.Duration;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.UIOOf;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Timer;

public interface UIOInstances {

  static Functor<UIO_> functor() {
    return UIOFunctor.INSTANCE;
  }

  static Applicative<UIO_> applicative() {
    return UIOApplicative.INSTANCE;
  }

  static Monad<UIO_> monad() {
    return UIOMonad.INSTANCE;
  }

  static MonadError<UIO_, Throwable> monadError() {
    return UIOMonadError.INSTANCE;
  }

  static MonadThrow<UIO_> monadThrow() {
    return UIOMonadThrow.INSTANCE;
  }

  static Timer<UIO_> timer() {
    return UIOTimer.INSTANCE;
  }

  static MonadDefer<UIO_> monadDefer() {
    return UIOMonadDefer.INSTANCE;
  }
}

interface UIOFunctor extends Functor<UIO_> {

  UIOFunctor INSTANCE = new UIOFunctor() {};

  @Override
  default <A, B> UIO<B> map(Kind<UIO_, A> value, Function1<A, B> map) {
    return UIOOf.narrowK(value).map(map);
  }
}

interface UIOPure extends Applicative<UIO_> {

  @Override
  default <A> UIO<A> pure(A value) {
    return UIO.pure(value);
  }
}

interface UIOApplicative extends UIOPure {

  UIOApplicative INSTANCE = new UIOApplicative() {};

  @Override
  default <A, B> UIO<B> ap(Kind<UIO_, A> value, Kind<UIO_, Function1<A, B>> apply) {
    return UIOOf.narrowK(apply).flatMap(map -> UIOOf.narrowK(value).map(map));
  }
}

interface UIOMonad extends UIOPure, Monad<UIO_> {

  UIOMonad INSTANCE = new UIOMonad() {};

  @Override
  default <A, B> UIO<B> flatMap(Kind<UIO_, A> value, Function1<A, ? extends Kind<UIO_, B>> map) {
    return value.fix(toUIO()).flatMap(map.andThen(UIOOf::narrowK));
  }
}

interface UIOMonadError extends UIOMonad, MonadError<UIO_, Throwable> {

  UIOMonadError INSTANCE = new UIOMonadError() {};

  @Override
  default <A> UIO<A> raiseError(Throwable error) {
    return UIO.<A>raiseError(error);
  }

  @Override
  default <A> UIO<A>
          handleErrorWith(Kind<UIO_, A> value,
                          Function1<Throwable, ? extends Kind<UIO_, A>> handler) {
    Function1<Throwable, UIO<A>> mapError = handler.andThen(UIOOf::narrowK);
    Function1<A, UIO<A>> map = UIO::pure;
    UIO<A> uio = UIOOf.narrowK(value);
    return uio.redeemWith(mapError, map);
  }
}

interface UIOMonadThrow extends UIOMonadError, MonadThrow<UIO_> {

  UIOMonadThrow INSTANCE = new UIOMonadThrow() {};
}

interface UIODefer extends Defer<UIO_> {

  @Override
  default <A> UIO<A>
          defer(Producer<Kind<UIO_, A>> defer) {
    return UIO.defer(() -> defer.map(UIOOf::narrowK).get());
  }
}

interface UIOBracket extends Bracket<UIO_> {

  @Override
  default <A, B> UIO<B>
          bracket(Kind<UIO_, A> acquire,
                  Function1<A, ? extends Kind<UIO_, B>> use,
                  Consumer1<A> release) {
    return UIO.bracket(acquire.fix(toUIO()), use.andThen(UIOOf::narrowK), release);
  }
}

interface UIOTimer extends Timer<UIO_> {
  
  UIOTimer INSTANCE = new UIOTimer() {};

  @Override
  default UIO<Unit> sleep(Duration duration) {
    return UIO.sleep(duration);
  }
}

interface UIOMonadDefer
    extends MonadDefer<UIO_>, UIOMonadThrow, UIODefer, UIOBracket, UIOTimer {

  UIOMonadDefer INSTANCE = new UIOMonadDefer() {};
}
