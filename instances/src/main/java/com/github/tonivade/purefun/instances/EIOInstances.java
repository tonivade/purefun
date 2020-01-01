/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.effect.EIO;

public interface EIOInstances {

  static <E> Functor<Higher1<EIO.µ, E>> functor() {
    return EIOFunctor.instance();
  }

  static <E> Applicative<Higher1<EIO.µ, E>> applicative() {
    return EIOApplicative.instance();
  }

  static <E> Monad<Higher1<EIO.µ, E>> monad() {
    return EIOMonad.instance();
  }

  static <E> MonadError<Higher1<EIO.µ, E>, E> monadError() {
    return EIOMonadError.instance();
  }

  static MonadThrow<Higher1<EIO.µ, Throwable>> monadThrow() {
    return EIOMonadThrow.instance();
  }

  static MonadDefer<Higher1<EIO.µ, Throwable>> monadDefer() {
    return EIOMonadDefer.instance();
  }

  static <A> Reference<Higher1<EIO.µ, Throwable>, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
}

@Instance
interface EIOFunctor<E> extends Functor<Higher1<EIO.µ, E>> {

  @Override
  default <A, B> Higher2<EIO.µ, E, B>
          map(Higher1<Higher1<EIO.µ, E>, A> value, Function1<A, B> map) {
    return EIO.narrowK(value).map(map).kind2();
  }
}

interface EIOPure<E> extends Applicative<Higher1<EIO.µ, E>> {

  @Override
  default <A> Higher2<EIO.µ, E, A> pure(A value) {
    return EIO.<E, A>pure(value).kind2();
  }
}

@Instance
interface EIOApplicative<E> extends EIOPure<E> {

  @Override
  default <A, B> Higher2<EIO.µ, E, B>
          ap(Higher1<Higher1<EIO.µ, E>, A> value,
             Higher1<Higher1<EIO.µ, E>, Function1<A, B>> apply) {
    return EIO.narrowK(apply).flatMap(map -> EIO.narrowK(value).map(map)).kind2();
  }
}

@Instance
interface EIOMonad<E> extends EIOPure<E>, Monad<Higher1<EIO.µ, E>> {

  @Override
  default <A, B> Higher2<EIO.µ, E, B>
          flatMap(Higher1<Higher1<EIO.µ, E>, A> value,
                  Function1<A, ? extends Higher1<Higher1<EIO.µ, E>, B>> map) {
    return EIO.narrowK(value).flatMap(map.andThen(EIO::narrowK)).kind2();
  }
}

@Instance
interface EIOMonadError<E> extends EIOMonad<E>, MonadError<Higher1<EIO.µ, E>, E> {

  @Override
  default <A> Higher2<EIO.µ, E, A> raiseError(E error) {
    return EIO.<E, A>raiseError(error).kind2();
  }

  @Override
  default <A> Higher2<EIO.µ, E, A>
          handleErrorWith(Higher1<Higher1<EIO.µ,  E>, A> value,
                          Function1<E, ? extends Higher1<Higher1<EIO.µ, E>, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<E, EIO<E, A>> mapError = handler.andThen(EIO::narrowK);
    Function1<A, EIO<E, A>> map = EIO::pure;
    EIO<E, A> eio = EIO.narrowK(value);
    return eio.foldM(mapError, map).kind2();
  }
}

@Instance
interface EIOMonadThrow
    extends EIOMonadError<Throwable>,
            MonadThrow<Higher1<EIO.µ, Throwable>> { }

interface EIODefer extends Defer<Higher1<EIO.µ, Throwable>> {

  @Override
  default <A> Higher2<EIO.µ, Throwable, A>
          defer(Producer<Higher1<Higher1<EIO.µ, Throwable>, A>> defer) {
    return EIO.defer(() -> defer.map(EIO::narrowK).get()).kind2();
  }
}

interface EIOBracket extends Bracket<Higher1<EIO.µ, Throwable>> {

  @Override
  default <A, B> Higher2<EIO.µ, Throwable, B>
          bracket(Higher1<Higher1<EIO.µ, Throwable>, A> acquire,
                  Function1<A, ? extends Higher1<Higher1<EIO.µ, Throwable>, B>> use,
                  Consumer1<A> release) {
    return EIO.bracket(acquire.fix1(EIO::narrowK), use.andThen(EIO::narrowK), release).kind2();
  }
}

@Instance
interface EIOMonadDefer
    extends MonadDefer<Higher1<EIO.µ, Throwable>>,
            EIOMonadThrow,
            EIODefer,
            EIOBracket { }
