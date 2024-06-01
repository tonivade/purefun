/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Function1.cons;
import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Unit.unit;
import java.time.Duration;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.transformer.EitherTOf;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.typeclasses.Timer;

public interface EitherTInstances {

  static <F extends Kind<F, ?>, L, R> Eq<Kind<EitherT<F, L, ?>, R>> eq(Eq<Kind<F, Either<L, R>>> eq) {
    return (a, b) -> eq.eqv(EitherTOf.toEitherT(a).value(), EitherTOf.toEitherT(b).value());
  }

  static <F extends Kind<F, ?>, L> Monad<EitherT<F, L, ?>> monad(Monad<F> monadF) {
    return EitherTMonad.instance(checkNonNull(monadF));
  }

  static <F extends Kind<F, ?>, L> MonadError<EitherT<F, L, ?>, L> monadError(Monad<F> monadF) {
    return EitherTMonadErrorFromMonad.instance(checkNonNull(monadF));
  }

  static <F extends Kind<F, ?>, L> MonadError<EitherT<F, L, ?>, L> monadError(MonadError<F, L> monadErrorF) {
    return EitherTMonadErrorFromMonadError.instance(checkNonNull(monadErrorF));
  }

  static <F extends Kind<F, ?>> MonadThrow<EitherT<F, Throwable, ?>> monadThrow(Monad<F> monadF) {
    return EitherTMonadThrowFromMonad.instance(checkNonNull(monadF));
  }

  static <F extends Kind<F, ?>> MonadThrow<EitherT<F, Throwable, ?>> monadThrow(MonadThrow<F> monadF) {
    return EitherTMonadThrowFromMonadThrow.instance(checkNonNull(monadF));
  }

  static <F extends Kind<F, ?>> MonadDefer<EitherT<F, Throwable, ?>> monadDefer(MonadDefer<F> monadDeferF) {
    return EitherTMonadDefer.instance(checkNonNull(monadDeferF));
  }

  static <F extends Kind<F, ?>, A>
         Reference<EitherT<F, Throwable, ?>, A>
         refFromMonadThrow(MonadDefer<F> monadDeferF, A value) {
    return Reference.of(monadDefer(monadDeferF), value);
  }
}

interface EitherTMonad<F extends Kind<F, ?>, L> extends Monad<EitherT<F, L, ?>> {

  static <F extends Kind<F, ?>, L> EitherTMonad<F, L> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> EitherT<F, L, T> pure(T value) {
    return EitherT.right(monadF(), value);
  }

  @Override
  default <T, R> EitherT<F, L, R> flatMap(Kind<EitherT<F, L, ?>, ? extends T> value,
      Function1<? super T, ? extends Kind<EitherT<F, L, ?>, ? extends R>> map) {
    return EitherTOf.toEitherT(value).flatMap(map.andThen(EitherTOf::toEitherT));
  }
}

interface EitherTMonadErrorFromMonad<F extends Kind<F, ?>, E>
    extends MonadError<EitherT<F, E, ?>, E>, EitherTMonad<F, E> {

  static <F extends Kind<F, ?>, L> EitherTMonadErrorFromMonad<F, L> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default <A> EitherT<F, E, A> raiseError(E error) {
    return EitherT.left(monadF(), error);
  }

  @Override
  default <A> EitherT<F, E, A> handleErrorWith(Kind<EitherT<F, E, ?>, A> value,
      Function1<? super E, ? extends Kind<EitherT<F, E, ?>, ? extends A>> handler) {
    return EitherT.of(monadF(),
        monadF().flatMap(EitherTOf.toEitherT(value).value(),
            either -> either.fold(
                e -> handler.andThen(EitherTOf::<F, E, A>toEitherT).apply(e).value(),
                a -> monadF().pure(Either.right(a)))));
  }
}

interface EitherTMonadErrorFromMonadError<F extends Kind<F, ?>, E>
    extends MonadError<EitherT<F, E, ?>, E>,
            EitherTMonad<F, E> {

  static <F extends Kind<F, ?>, E> EitherTMonadErrorFromMonadError<F, E> instance(MonadError<F, E> monadErrorF) {
    return () -> monadErrorF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> EitherT<F, E, A> raiseError(E error) {
    return EitherT.of(monadF(), monadF().raiseError(error));
  }

  @Override
  default <A> EitherT<F, E, A> handleErrorWith(Kind<EitherT<F, E, ?>, A> value,
      Function1<? super E, ? extends Kind<EitherT<F, E, ?>, ? extends A>> handler) {
    return EitherT.of(monadF(),
                      monadF().handleErrorWith(EitherTOf.toEitherT(value).value(),
                                               error -> handler.andThen(EitherTOf::<F, E, A>toEitherT).apply(error).value()));
  }
}

interface EitherTMonadThrowFromMonad<F extends Kind<F, ?>>
    extends EitherTMonadErrorFromMonad<F, Throwable>,
            MonadThrow<EitherT<F, Throwable, ?>> {

  static <F extends Kind<F, ?>> EitherTMonadThrowFromMonad<F> instance(Monad<F> monadF) {
    return () -> monadF;
  }
}

interface EitherTMonadThrowFromMonadThrow<F extends Kind<F, ?>>
    extends EitherTMonadErrorFromMonadError<F, Throwable>,
            MonadThrow<EitherT<F, Throwable, ?>> {

  static <F extends Kind<F, ?>> EitherTMonadThrowFromMonadThrow<F> instance(MonadThrow<F> monadThrowF) {
    return () -> monadThrowF;
  }
}

interface EitherTDefer<F extends Kind<F, ?>, E> extends Defer<EitherT<F, E, ?>> {

  MonadDefer<F> monadF();

  @Override
  default <A> EitherT<F, E, A> defer(Producer<? extends Kind<EitherT<F, E, ?>, ? extends A>> defer) {
    return EitherT.of(monadF(), monadF().defer(() -> defer.map(EitherTOf::<F, E, A>toEitherT).get().value()));
  }
}

interface EitherTBracket<F extends Kind<F, ?>, E> extends Bracket<EitherT<F, E, ?>, E> {

  MonadDefer<F> monadF();

  <A> Kind<F, Either<E, A>> acquireRecover(E error);

  @Override
  default <A, B> EitherT<F, E, B>
          bracket(Kind<EitherT<F, E, ?>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<EitherT<F, E, ?>, ? extends B>> use,
                  Function1<? super A, ? extends Kind<EitherT<F, E, ?>, Unit>> release) {
    Kind<F, Either<E, B>> bracket =
        monadF().bracket(
            acquire.fix(EitherTOf::<F, E, A>toEitherT).value(),
            either -> either.fold(
                this::acquireRecover,
                value -> use.andThen(EitherTOf::<F, E, B>toEitherT).apply(value).value()),
            either -> {
              Kind<EitherT<F, E, ?>, Unit> fold = either.fold(error -> EitherT.left(monadF(), error), release);
              Kind<F, Either<E, Unit>> value = fold.fix(EitherTOf::<F, E, Unit>toEitherT).value();
              return monadF().map(value, x -> x.fold(cons(unit()), identity()));
            });
    return EitherT.of(monadF(), bracket);
  }
}

interface EitherTTimer<F extends Kind<F, ?>> extends Timer<EitherT<F, Throwable, ?>> {

  MonadDefer<F> monadF();

  @Override
  default EitherT<F, Throwable, Unit> sleep(Duration duration) {
    return EitherT.of(monadF(), monadF().map(monadF().sleep(duration), Either::right));
  }
}

interface EitherTMonadDefer<F extends Kind<F, ?>>
    extends EitherTMonadThrowFromMonadThrow<F>,
            EitherTDefer<F, Throwable>,
            EitherTBracket<F, Throwable>,
            EitherTTimer<F>,
            MonadDefer<EitherT<F, Throwable, ?>> {

  static <F extends Kind<F, ?>> EitherTMonadDefer<F> instance(MonadDefer<F> monadDeferF) {
    return () -> monadDeferF;
  }

  @Override
  default <A> Kind<F, Either<Throwable, A>> acquireRecover(Throwable error) {
    return monadF().raiseError(error);
  }
}
