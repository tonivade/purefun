/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.time.Duration;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.EIO;
import com.github.tonivade.purefun.effect.EIOOf;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;

  @SuppressWarnings("unchecked")
public interface EIOInstances {

  static <E> Functor<Kind<EIO_, E>> functor() {
    return EIOFunctor.INSTANCE;
  }

  static <E> Applicative<Kind<EIO_, E>> applicative() {
    return EIOApplicative.INSTANCE;
  }

  static <E> Monad<Kind<EIO_, E>> monad() {
    return EIOMonad.INSTANCE;
  }

  static <E> MonadError<Kind<EIO_, E>, E> monadError() {
    return EIOMonadError.INSTANCE;
  }

  static MonadThrow<Kind<EIO_, Throwable>> monadThrow() {
    return EIOMonadThrow.INSTANCE;
  }

  static MonadDefer<Kind<EIO_, Throwable>> monadDefer() {
    return EIOMonadDefer.INSTANCE;
  }

  static <A> Reference<Kind<EIO_, Throwable>, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
}

interface EIOFunctor<E> extends Functor<Kind<EIO_, E>> {

  @SuppressWarnings("rawtypes")
  EIOFunctor INSTANCE = new EIOFunctor() {};

  @Override
  default <A, B> EIO<E, B>
          map(Kind<Kind<EIO_, E>, A> value, Function1<A, B> map) {
    return EIOOf.narrowK(value).map(map);
  }
}

interface EIOPure<E> extends Applicative<Kind<EIO_, E>> {

  @Override
  default <A> EIO<E, A> pure(A value) {
    return EIO.<E, A>pure(value);
  }
}

interface EIOApplicative<E> extends EIOPure<E> {

  @SuppressWarnings("rawtypes")
  EIOApplicative INSTANCE = new EIOApplicative() {};

  @Override
  default <A, B> EIO<E, B>
          ap(Kind<Kind<EIO_, E>, A> value,
             Kind<Kind<EIO_, E>, Function1<A, B>> apply) {
    return EIOOf.narrowK(apply).flatMap(map -> EIOOf.narrowK(value).map(map));
  }
}

interface EIOMonad<E> extends EIOPure<E>, Monad<Kind<EIO_, E>> {

  @SuppressWarnings("rawtypes")
  EIOMonad INSTANCE = new EIOMonad() {};

  @Override
  default <A, B> EIO<E, B>
          flatMap(Kind<Kind<EIO_, E>, A> value,
                  Function1<A, ? extends Kind<Kind<EIO_, E>, B>> map) {
    return EIOOf.narrowK(value).flatMap(map.andThen(EIOOf::narrowK));
  }
}

interface EIOMonadError<E> extends EIOMonad<E>, MonadError<Kind<EIO_, E>, E> {

  @SuppressWarnings("rawtypes")
  EIOMonadError INSTANCE = new EIOMonadError() {};

  @Override
  default <A> EIO<E, A> raiseError(E error) {
    return EIO.<E, A>raiseError(error);
  }

  @Override
  default <A> EIO<E, A>
          handleErrorWith(Kind<Kind<EIO_,  E>, A> value,
                          Function1<E, ? extends Kind<Kind<EIO_, E>, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<E, EIO<E, A>> mapError = handler.andThen(EIOOf::narrowK);
    Function1<A, EIO<E, A>> map = EIO::pure;
    EIO<E, A> eio = EIOOf.narrowK(value);
    return eio.foldM(mapError, map);
  }
}

interface EIOMonadThrow
    extends EIOMonadError<Throwable>,
            MonadThrow<Kind<EIO_, Throwable>> {

  EIOMonadThrow INSTANCE = new EIOMonadThrow() {};
}

interface EIODefer extends Defer<Kind<EIO_, Throwable>> {

  @Override
  default <A> EIO<Throwable, A>
          defer(Producer<Kind<Kind<EIO_, Throwable>, A>> defer) {
    return EIO.defer(() -> defer.map(EIOOf::narrowK).get());
  }
}

interface EIOBracket extends Bracket<Kind<EIO_, Throwable>> {

  @Override
  default <A, B> EIO<Throwable, B>
          bracket(Kind<Kind<EIO_, Throwable>, A> acquire,
                  Function1<A, ? extends Kind<Kind<EIO_, Throwable>, B>> use,
                  Consumer1<A> release) {
    return EIO.bracket(acquire.fix(EIOOf::narrowK), use.andThen(EIOOf::narrowK), release);
  }
}

interface EIOMonadDefer
    extends MonadDefer<Kind<EIO_, Throwable>>, EIOMonadThrow, EIODefer, EIOBracket {

  EIOMonadDefer INSTANCE = new EIOMonadDefer() {};

  @Override
  default EIO<Throwable, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).<Throwable>toEIO();
  }
}
