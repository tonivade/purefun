/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Function1.cons;
import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Unit.unit;
import static com.github.tonivade.purefun.transformer.EitherTOf.toEitherT;

import java.time.Duration;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.transformer.EitherTOf;
import com.github.tonivade.purefun.transformer.EitherT_;
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

  static <F, L, R> Eq<Kind<Kind<Kind<EitherT_, F>, L>, R>> eq(Eq<Kind<F, Either<L, R>>> eq) {
    return (a, b) -> eq.eqv(EitherTOf.narrowK(a).value(), EitherTOf.narrowK(b).value());
  }

  static <F, L> Monad<Kind<Kind<EitherT_, F>, L>> monad(Monad<F> monadF) {
    return EitherTMonad.instance(checkNonNull(monadF));
  }

  static <F, L> MonadError<Kind<Kind<EitherT_, F>, L>, L> monadError(Monad<F> monadF) {
    return EitherTMonadErrorFromMonad.instance(checkNonNull(monadF));
  }

  static <F, L> MonadError<Kind<Kind<EitherT_, F>, L>, L> monadError(MonadError<F, L> monadErrorF) {
    return EitherTMonadErrorFromMonadError.instance(checkNonNull(monadErrorF));
  }

  static <F> MonadThrow<Kind<Kind<EitherT_, F>, Throwable>> monadThrow(Monad<F> monadF) {
    return EitherTMonadThrowFromMonad.instance(checkNonNull(monadF));
  }

  static <F> MonadThrow<Kind<Kind<EitherT_, F>, Throwable>> monadThrow(MonadThrow<F> monadF) {
    return EitherTMonadThrowFromMonadThrow.instance(checkNonNull(monadF));
  }

  static <F> MonadDefer<Kind<Kind<EitherT_, F>, Throwable>> monadDefer(MonadDefer<F> monadDeferF) {
    return EitherTMonadDefer.instance(checkNonNull(monadDeferF));
  }

  static <F, A>
         Reference<Kind<Kind<EitherT_, F>, Throwable>, A>
         refFromMonadThrow(MonadDefer<F> monadDeferF, A value) {
    return Reference.of(monadDefer(monadDeferF), value);
  }
}

interface EitherTMonad<F, L> extends Monad<Kind<Kind<EitherT_, F>, L>> {

  static <F, L> EitherTMonad<F, L> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> EitherT<F, L, T> pure(T value) {
    return EitherT.right(monadF(), value);
  }

  @Override
  default <T, R> EitherT<F, L, R> flatMap(Kind<Kind<Kind<EitherT_, F>, L>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<Kind<EitherT_, F>, L>, ? extends R>> map) {
    return EitherTOf.narrowK(value).flatMap(map.andThen(EitherTOf::narrowK));
  }
}

interface EitherTMonadErrorFromMonad<F, E>
    extends MonadError<Kind<Kind<EitherT_, F>, E>, E>, EitherTMonad<F, E> {

  static <F, L> EitherTMonadErrorFromMonad<F, L> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default <A> EitherT<F, E, A> raiseError(E error) {
    return EitherT.left(monadF(), error);
  }

  @Override
  default <A> EitherT<F, E, A> handleErrorWith(Kind<Kind<Kind<EitherT_, F>, E>, A> value,
      Function1<? super E, ? extends Kind<Kind<Kind<EitherT_, F>, E>, ? extends A>> handler) {
    return EitherT.of(monadF(),
        monadF().flatMap(EitherTOf.narrowK(value).value(),
            either -> either.fold(
                e -> handler.andThen(EitherTOf::<F, E, A>narrowK).apply(e).value(),
                a -> monadF().pure(Either.right(a)))));
  }
}

interface EitherTMonadErrorFromMonadError<F, E>
    extends MonadError<Kind<Kind<EitherT_, F>, E>, E>,
            EitherTMonad<F, E> {

  static <F, E> EitherTMonadErrorFromMonadError<F, E> instance(MonadError<F, E> monadErrorF) {
    return () -> monadErrorF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> EitherT<F, E, A> raiseError(E error) {
    return EitherT.of(monadF(), monadF().raiseError(error));
  }

  @Override
  default <A> EitherT<F, E, A> handleErrorWith(Kind<Kind<Kind<EitherT_, F>, E>, A> value,
      Function1<? super E, ? extends Kind<Kind<Kind<EitherT_, F>, E>, ? extends A>> handler) {
    return EitherT.of(monadF(),
                      monadF().handleErrorWith(EitherTOf.narrowK(value).value(),
                                               error -> handler.andThen(EitherTOf::<F, E, A>narrowK).apply(error).value()));
  }
}

interface EitherTMonadThrowFromMonad<F>
    extends EitherTMonadErrorFromMonad<F, Throwable>,
            MonadThrow<Kind<Kind<EitherT_, F>, Throwable>> {

  static <F> EitherTMonadThrowFromMonad<F> instance(Monad<F> monadF) {
    return () -> monadF;
  }
}

interface EitherTMonadThrowFromMonadThrow<F>
    extends EitherTMonadErrorFromMonadError<F, Throwable>,
            MonadThrow<Kind<Kind<EitherT_, F>, Throwable>> {

  static <F> EitherTMonadThrowFromMonadThrow<F> instance(MonadThrow<F> monadThrowF) {
    return () -> monadThrowF;
  }
}

interface EitherTDefer<F, E> extends Defer<Kind<Kind<EitherT_, F>, E>> {

  MonadDefer<F> monadF();

  @Override
  default <A> EitherT<F, E, A> defer(Producer<? extends Kind<Kind<Kind<EitherT_, F>, E>, ? extends A>> defer) {
    return EitherT.of(monadF(), monadF().defer(() -> defer.map(EitherTOf::<F, E, A>narrowK).get().value()));
  }
}

interface EitherTBracket<F, E> extends Bracket<Kind<Kind<EitherT_, F>, E>, E> {

  MonadDefer<F> monadF();

  <A> Kind<F, Either<E, A>> acquireRecover(E error);

  @Override
  default <A, B> EitherT<F, E, B>
          bracket(Kind<Kind<Kind<EitherT_, F>, E>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<Kind<Kind<EitherT_, F>, E>, ? extends B>> use,
                  Function1<? super A, ? extends Kind<Kind<Kind<EitherT_, F>, E>, Unit>> release) {
    Kind<F, Either<E, B>> bracket =
        monadF().bracket(
            acquire.fix(EitherTOf::<F, E, A>narrowK).value(),
            either -> either.fold(
                this::acquireRecover,
                value -> use.andThen(EitherTOf::<F, E, B>narrowK).apply(value).value()),
            either -> {
              Kind<Kind<Kind<EitherT_, F>, E>, Unit> fold = either.fold(error -> EitherT.left(monadF(), error), release);
              Kind<F, Either<E, Unit>> value = fold.fix(toEitherT()).value();
              return monadF().map(value, x -> x.fold(cons(unit()), identity()));
            });
    return EitherT.of(monadF(), bracket);
  }
}

interface EitherTTimer<F> extends Timer<Kind<Kind<EitherT_, F>, Throwable>> {

  MonadDefer<F> monadF();

  @Override
  default EitherT<F, Throwable, Unit> sleep(Duration duration) {
    return EitherT.of(monadF(), monadF().map(monadF().sleep(duration), Either::right));
  }
}

interface EitherTMonadDefer<F>
    extends EitherTMonadThrowFromMonadThrow<F>,
            EitherTDefer<F, Throwable>,
            EitherTBracket<F, Throwable>,
            EitherTTimer<F>,
            MonadDefer<Kind<Kind<EitherT_, F>, Throwable>> {

  static <F> EitherTMonadDefer<F> instance(MonadDefer<F> monadDeferF) {
    return () -> monadDeferF;
  }

  @Override
  default <A> Kind<F, Either<Throwable, A>> acquireRecover(Throwable error) {
    return monadF().raiseError(error);
  }
}
