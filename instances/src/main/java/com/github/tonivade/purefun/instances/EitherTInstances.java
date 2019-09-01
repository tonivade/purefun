/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;

public interface EitherTInstances {

  static <F extends Kind, L, R> Eq<Higher3<EitherT.µ, F, L, R>> eq(Eq<Higher1<F, Either<L, R>>> eq) {
    return (a, b) -> eq.eqv(EitherT.narrowK(a).value(), EitherT.narrowK(b).value());
  }

  static <F extends Kind, L> Monad<Higher1<Higher1<EitherT.µ, F>, L>> monad(Monad<F> monadF) {
    return EitherTMonad.instance(requireNonNull(monadF));
  }

  static <F extends Kind, L> MonadError<Higher1<Higher1<EitherT.µ, F>, L>, L> monadError(Monad<F> monadF) {
    return EitherTMonadErrorFromMonad.instance(requireNonNull(monadF));
  }

  static <F extends Kind, L> MonadError<Higher1<Higher1<EitherT.µ, F>, L>, L> monadError(MonadError<F, L> monadErrorF) {
    return EitherTMonadErrorFromMonadError.instance(requireNonNull(monadErrorF));
  }

  static <F extends Kind> MonadThrow<Higher1<Higher1<EitherT.µ, F>, Throwable>> monadThrow(Monad<F> monadF) {
    return EitherTMonadThrowFromMonad.instance(requireNonNull(monadF));
  }

  static <F extends Kind> MonadThrow<Higher1<Higher1<EitherT.µ, F>, Throwable>> monadThrow(MonadThrow<F> monadF) {
    return EitherTMonadThrowFromMonadThrow.instance(requireNonNull(monadF));
  }

  static <F extends Kind, L> Defer<Higher1<Higher1<EitherT.µ, F>, L>> defer(MonadDefer<F> monadDeferF) {
    return EitherTDefer.instance(requireNonNull(monadDeferF));
  }

  static <F extends Kind> MonadDefer<Higher1<Higher1<EitherT.µ, F>, Throwable>> monadDeferFromMonad(MonadDefer<F> monadDeferF) {
    return EitherTMonadDeferFromMonad.instance(requireNonNull(monadDeferF));
  }

  static <F extends Kind> MonadDefer<Higher1<Higher1<EitherT.µ, F>, Throwable>> monadDeferFromMonadThrow(MonadDefer<F> monadDeferF) {
    return EitherTMonadDeferFromMonadThrow.instance(requireNonNull(monadDeferF));
  }

  static <F extends Kind, A>
         Reference<Higher1<Higher1<EitherT.µ, F>, Throwable>, A>
         refFromMonad(MonadDefer<F> monadDeferF, A value) {
    return Reference.of(monadDeferFromMonad(monadDeferF), value);
  }

  static <F extends Kind, A>
         Reference<Higher1<Higher1<EitherT.µ, F>, Throwable>, A>
         refFromMonadThrow(MonadDefer<F> monadDeferF, A value) {
    return Reference.of(monadDeferFromMonadThrow(monadDeferF), value);
  }
}

@Instance
interface EitherTMonad<F extends Kind, L> extends Monad<Higher1<Higher1<EitherT.µ, F>, L>> {

  static <F extends Kind, L> EitherTMonad<F, L> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> Higher3<EitherT.µ, F, L, T> pure(T value) {
    return EitherT.<F, L, T>right(monadF(), value).kind3();
  }

  @Override
  default <T, R> Higher3<EitherT.µ, F, L, R> flatMap(Higher1<Higher1<Higher1<EitherT.µ, F>, L>, T> value,
      Function1<T, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, L>, R>> map) {
    return EitherT.narrowK(value).flatMap(map.andThen(EitherT::<F, L, R>narrowK)).kind3();
  }
}

@Instance
interface EitherTMonadErrorFromMonad<F extends Kind, E>
    extends MonadError<Higher1<Higher1<EitherT.µ, F>, E>, E>, EitherTMonad<F, E> {

  static <F extends Kind, L> EitherTMonadErrorFromMonad<F, L> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default <A> Higher3<EitherT.µ, F, E, A> raiseError(E error) {
    return EitherT.<F, E, A>left(monadF(), error).kind3();
  }

  @Override
  default <A> Higher3<EitherT.µ, F, E, A> handleErrorWith(Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A> value,
      Function1<E, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A>> handler) {
    return EitherT.of(monadF(),
        monadF().flatMap(EitherT.narrowK(value).value(),
            either -> either.fold(
                e -> handler.andThen(EitherT::<F, E, A>narrowK).apply(e).value(),
                a -> monadF().pure(Either.<E, A>right(a))).kind1())).kind3();
  }
}

@Instance
interface EitherTMonadErrorFromMonadError<F extends Kind, E>
    extends MonadError<Higher1<Higher1<EitherT.µ, F>, E>, E>,
            EitherTMonad<F, E> {

  static <F extends Kind, E> EitherTMonadErrorFromMonadError<F, E> instance(MonadError<F, E> monadErrorF) {
    return () -> monadErrorF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> Higher3<EitherT.µ, F, E, A> raiseError(E error) {
    return EitherT.<F, E, A>of(monadF(), monadF().raiseError(error)).kind3();
  }

  @Override
  default <A> Higher3<EitherT.µ, F, E, A> handleErrorWith(Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A> value,
      Function1<E, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A>> handler) {
    return EitherT.of(monadF(),
                      monadF().handleErrorWith(EitherT.narrowK(value).value(),
                                               error -> handler.andThen(EitherT::<F, E, A>narrowK).apply(error).value())).kind3();
  }
}

@Instance
interface EitherTMonadThrowFromMonad<F extends Kind>
    extends EitherTMonadErrorFromMonad<F, Throwable>,
            MonadThrow<Higher1<Higher1<EitherT.µ, F>, Throwable>> {

  static <F extends Kind> EitherTMonadThrowFromMonad<F> instance(Monad<F> monadF) {
    return () -> monadF;
  }
}

@Instance
interface EitherTMonadThrowFromMonadThrow<F extends Kind>
    extends EitherTMonadErrorFromMonadError<F, Throwable>,
            MonadThrow<Higher1<Higher1<EitherT.µ, F>, Throwable>> {

  static <F extends Kind> EitherTMonadThrowFromMonadThrow<F> instance(MonadThrow<F> monadThrowF) {
    return () -> monadThrowF;
  }
}

@Instance
interface EitherTDefer<F extends Kind, E> extends Defer<Higher1<Higher1<EitherT.µ, F>, E>> {

  static <F extends Kind, E> EitherTDefer<F, E> instance(MonadDefer<F> monadDeferF) {
    return () -> monadDeferF;
  }

  MonadDefer<F> monadF();

  @Override
  default <A> Higher3<EitherT.µ, F, E, A> defer(Producer<Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A>> defer) {
    return EitherT.of(monadF(), monadF().defer(() -> defer.map(EitherT::<F, E, A>narrowK).get().value())).kind3();
  }
}

@Instance
interface EitherTBracket<F extends Kind> extends Bracket<Higher1<Higher1<EitherT.µ, F>, Throwable>> {

  MonadDefer<F> monadF();

  <A> Higher1<F, Either<Throwable, A>> acquireRecover(Throwable error);

  @Override
  default <A, B> Higher3<EitherT.µ, F, Throwable, B>
          bracket(Higher1<Higher1<Higher1<EitherT.µ, F>, Throwable>, A> acquire,
                  Function1<A, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, Throwable>, B>> use,
                  Consumer1<A> release) {
    Higher1<F, Either<Throwable, B>> bracket =
        monadF().bracket(
            acquire.fix1(EitherT::<F, Throwable, A>narrowK).value(),
            either -> either.fold(
                this::acquireRecover,
                value -> use.andThen(EitherT::<F, Throwable, B>narrowK).apply(value).value()),
            either -> either.fold(cons(unit()), release.asFunction()));
    return EitherT.of(monadF(), bracket).kind3();
  }
}

@Instance
interface EitherTMonadDeferFromMonad<F extends Kind>
    extends EitherTMonadThrowFromMonad<F>,
            EitherTDefer<F, Throwable>,
            EitherTBracket<F>,
            MonadDefer<Higher1<Higher1<EitherT.µ, F>, Throwable>> {

  static <F extends Kind> EitherTMonadDeferFromMonad<F> instance(MonadDefer<F> monadDeferF) {
    return () -> monadDeferF;
  }

  @Override
  default <A> Higher1<F, Either<Throwable, A>> acquireRecover(Throwable error) {
    return monadF().pure(Either.left(error));
  }
}

@Instance
interface EitherTMonadDeferFromMonadThrow<F extends Kind>
    extends EitherTMonadThrowFromMonadThrow<F>,
            EitherTDefer<F, Throwable>,
            EitherTBracket<F>,
            MonadDefer<Higher1<Higher1<EitherT.µ, F>, Throwable>> {

  static <F extends Kind> EitherTMonadDeferFromMonadThrow<F> instance(MonadDefer<F> monadDeferF) {
    return () -> monadDeferF;
  }

  @Override
  default <A> Higher1<F, Either<Throwable, A>> acquireRecover(Throwable error) {
    return monadF().raiseError(error);
  }
}
