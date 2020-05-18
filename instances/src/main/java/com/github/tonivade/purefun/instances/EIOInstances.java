/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.time.Duration;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.EIO;
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

  static <E> Functor<Higher1<EIO_, E>> functor() {
    return EIOFunctor.INSTANCE;
  }

  static <E> Applicative<Higher1<EIO_, E>> applicative() {
    return EIOApplicative.INSTANCE;
  }

  static <E> Monad<Higher1<EIO_, E>> monad() {
    return EIOMonad.INSTANCE;
  }

  static <E> MonadError<Higher1<EIO_, E>, E> monadError() {
    return EIOMonadError.INSTANCE;
  }

  static MonadThrow<Higher1<EIO_, Throwable>> monadThrow() {
    return EIOMonadThrow.INSTANCE;
  }

  static MonadDefer<Higher1<EIO_, Throwable>> monadDefer() {
    return EIOMonadDefer.INSTANCE;
  }

  static <A> Reference<Higher1<EIO_, Throwable>, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
}

interface EIOFunctor<E> extends Functor<Higher1<EIO_, E>> {

  @SuppressWarnings("rawtypes")
  EIOFunctor INSTANCE = new EIOFunctor() {};

  @Override
  default <A, B> Higher2<EIO_, E, B>
          map(Higher1<Higher1<EIO_, E>, A> value, Function1<A, B> map) {
    return EIO_.narrowK(value).map(map);
  }
}

interface EIOPure<E> extends Applicative<Higher1<EIO_, E>> {

  @Override
  default <A> Higher2<EIO_, E, A> pure(A value) {
    return EIO.<E, A>pure(value);
  }
}

interface EIOApplicative<E> extends EIOPure<E> {

  @SuppressWarnings("rawtypes")
  EIOApplicative INSTANCE = new EIOApplicative() {};

  @Override
  default <A, B> Higher2<EIO_, E, B>
          ap(Higher1<Higher1<EIO_, E>, A> value,
             Higher1<Higher1<EIO_, E>, Function1<A, B>> apply) {
    return EIO_.narrowK(apply).flatMap(map -> EIO_.narrowK(value).map(map));
  }
}

interface EIOMonad<E> extends EIOPure<E>, Monad<Higher1<EIO_, E>> {

  @SuppressWarnings("rawtypes")
  EIOMonad INSTANCE = new EIOMonad() {};

  @Override
  default <A, B> Higher2<EIO_, E, B>
          flatMap(Higher1<Higher1<EIO_, E>, A> value,
                  Function1<A, ? extends Higher1<Higher1<EIO_, E>, B>> map) {
    return EIO_.narrowK(value).flatMap(map.andThen(EIO_::narrowK));
  }
}

interface EIOMonadError<E> extends EIOMonad<E>, MonadError<Higher1<EIO_, E>, E> {

  @SuppressWarnings("rawtypes")
  EIOMonadError INSTANCE = new EIOMonadError() {};

  @Override
  default <A> Higher2<EIO_, E, A> raiseError(E error) {
    return EIO.<E, A>raiseError(error);
  }

  @Override
  default <A> Higher2<EIO_, E, A>
          handleErrorWith(Higher1<Higher1<EIO_,  E>, A> value,
                          Function1<E, ? extends Higher1<Higher1<EIO_, E>, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<E, EIO<E, A>> mapError = handler.andThen(EIO_::narrowK);
    Function1<A, EIO<E, A>> map = EIO::pure;
    EIO<E, A> eio = EIO_.narrowK(value);
    return eio.foldM(mapError, map);
  }
}

interface EIOMonadThrow
    extends EIOMonadError<Throwable>,
            MonadThrow<Higher1<EIO_, Throwable>> {

  EIOMonadThrow INSTANCE = new EIOMonadThrow() {};
}

interface EIODefer extends Defer<Higher1<EIO_, Throwable>> {

  @Override
  default <A> Higher2<EIO_, Throwable, A>
          defer(Producer<Higher1<Higher1<EIO_, Throwable>, A>> defer) {
    return EIO.defer(() -> defer.map(EIO_::narrowK).get());
  }
}

interface EIOBracket extends Bracket<Higher1<EIO_, Throwable>> {

  @Override
  default <A, B> Higher2<EIO_, Throwable, B>
          bracket(Higher1<Higher1<EIO_, Throwable>, A> acquire,
                  Function1<A, ? extends Higher1<Higher1<EIO_, Throwable>, B>> use,
                  Consumer1<A> release) {
    return EIO.bracket(acquire.fix1(EIO_::narrowK), use.andThen(EIO_::narrowK), release);
  }
}

interface EIOMonadDefer
    extends MonadDefer<Higher1<EIO_, Throwable>>, EIOMonadThrow, EIODefer, EIOBracket {

  EIOMonadDefer INSTANCE = new EIOMonadDefer() {};

  @Override
  default Higher2<EIO_, Throwable, Unit> sleep(Duration duration) {
    return UIO.sleep(duration).<Throwable>toEIO();
  }
}
