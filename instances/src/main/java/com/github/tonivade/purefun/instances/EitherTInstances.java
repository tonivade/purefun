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
    requireNonNull(monadF);
    return new EitherTMonad<F, L>() {

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }

  static <F extends Kind, L> MonadError<Higher1<Higher1<EitherT.µ, F>, L>, L> monadError(Monad<F> monadF) {
    requireNonNull(monadF);
    return new EitherTMonadErrorFromMonad<F, L>() {

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }

  static <F extends Kind, L> MonadError<Higher1<Higher1<EitherT.µ, F>, L>, L> monadError(MonadError<F, L> monadErrorF) {
    requireNonNull(monadErrorF);
    return new EitherTMonadErrorFromMonadError<F, L>() {

      @Override
      public MonadError<F, L> monadF() { return monadErrorF; }
    };
  }

  static <F extends Kind> MonadThrow<Higher1<Higher1<EitherT.µ, F>, Throwable>> monadThrow(Monad<F> monadF) {
    requireNonNull(monadF);
    return new EitherTMonadThrowFromMonad<F>() {

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }

  static <F extends Kind> MonadThrow<Higher1<Higher1<EitherT.µ, F>, Throwable>> monadThrow(MonadThrow<F> monadF) {
    requireNonNull(monadF);
    return new EitherTMonadThrowFromMonadThrow<F>() {

      @Override
      public MonadThrow<F> monadF() { return monadF; }
    };
  }

  static <F extends Kind, L> Defer<Higher1<Higher1<EitherT.µ, F>, L>> defer(MonadDefer<F> monadDeferF) {
    requireNonNull(monadDeferF);
    return new EitherTDefer<F, L>() {

      @Override
      public Monad<F> monadF() { return monadDeferF; }

      @Override
      public Defer<F> deferF() { return monadDeferF; }
    };
  }

  static <F extends Kind> MonadDefer<Higher1<Higher1<EitherT.µ, F>, Throwable>> monadDeferFromMonad(MonadDefer<F> monadDeferF) {
    requireNonNull(monadDeferF);
    return new EitherTMonadDeferFromMonad<F>() {

      @Override
      public Monad<F> monadF() { return monadDeferF; }

      @Override
      public Defer<F> deferF() { return monadDeferF; }

      @Override
      public Bracket<F> bracketF() { return monadDeferF; }

      @Override
      public <A> Higher1<F, Either<Throwable, A>> acquireRecover(Throwable error) {
        return monadDeferF.pure(Either.left(error));
      }
    };
  }

  static <F extends Kind> MonadDefer<Higher1<Higher1<EitherT.µ, F>, Throwable>> monadDeferFromMonadThrow(MonadDefer<F> monadDeferF) {
    requireNonNull(monadDeferF);
    return new EitherTMonadDeferFromMonadThrow<F>() {

      @Override
      public MonadThrow<F> monadF() { return monadDeferF; }

      @Override
      public Defer<F> deferF() { return monadDeferF; }

      @Override
      public Bracket<F> bracketF() { return monadDeferF; }

      @Override
      public <A> Higher1<F, Either<Throwable, A>> acquireRecover(Throwable error) {
        return monadDeferF.raiseError(error);
      }
    };
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

  Monad<F> monadF();

  @Override
  default <T> EitherT<F, L, T> pure(T value) {
    return EitherT.right(monadF(), value);
  }

  @Override
  default <T, R> EitherT<F, L, R> flatMap(Higher1<Higher1<Higher1<EitherT.µ, F>, L>, T> value,
      Function1<T, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, L>, R>> map) {
    return EitherT.narrowK(value).flatMap(map.andThen(EitherT::narrowK));
  }
}

@Instance
interface EitherTMonadErrorFromMonad<F extends Kind, E>
    extends MonadError<Higher1<Higher1<EitherT.µ, F>, E>, E>, EitherTMonad<F, E> {

  @Override
  default <A> EitherT<F, E, A> raiseError(E error) {
    return EitherT.left(monadF(), error);
  }

  @Override
  default <A> EitherT<F, E, A> handleErrorWith(Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A> value,
      Function1<E, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A>> handler) {
    return EitherT.of(monadF(),
        monadF().flatMap(EitherT.narrowK(value).value(),
            either -> either.fold(
                e -> handler.andThen(EitherT::narrowK).apply(e).value(),
                a -> monadF().pure(Either.right(a)))));
  }
}

@Instance
interface EitherTMonadErrorFromMonadError<F extends Kind, E>
    extends MonadError<Higher1<Higher1<EitherT.µ, F>, E>, E>,
            EitherTMonad<F, E> {

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> EitherT<F, E, A> raiseError(E error) {
    return EitherT.of(monadF(), monadF().raiseError(error));
  }

  @Override
  default <A> EitherT<F, E, A> handleErrorWith(Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A> value,
      Function1<E, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A>> handler) {
    return EitherT.of(monadF(),
                      monadF().handleErrorWith(EitherT.narrowK(value).value(),
                                               error -> handler.andThen(EitherT::narrowK).apply(error).value()));
  }
}

@Instance
interface EitherTMonadThrowFromMonad<F extends Kind>
    extends EitherTMonadErrorFromMonad<F, Throwable>,
            MonadThrow<Higher1<Higher1<EitherT.µ, F>, Throwable>> { }

@Instance
interface EitherTMonadThrowFromMonadThrow<F extends Kind>
    extends EitherTMonadErrorFromMonadError<F, Throwable>,
            MonadThrow<Higher1<Higher1<EitherT.µ, F>, Throwable>> { }

@Instance
interface EitherTDefer<F extends Kind, E> extends Defer<Higher1<Higher1<EitherT.µ, F>, E>> {

  Monad<F> monadF();
  Defer<F> deferF();

  @Override
  default <A> EitherT<F, E, A> defer(Producer<Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A>> defer) {
    return EitherT.of(monadF(), deferF().defer(() -> defer.map(EitherT::narrowK).get().value()));
  }
}

@Instance
interface EitherTBracket<F extends Kind> extends Bracket<Higher1<Higher1<EitherT.µ, F>, Throwable>> {

  Bracket<F> bracketF();
  Monad<F> monadF();
  <A> Higher1<F, Either<Throwable, A>> acquireRecover(Throwable error);

  @Override
  default <A, B> EitherT<F, Throwable, B>
          bracket(Higher1<Higher1<Higher1<EitherT.µ, F>, Throwable>, A> acquire,
                  Function1<A, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, Throwable>, B>> use,
                  Consumer1<A> release) {
    Higher1<F, Either<Throwable, B>> bracket =
        bracketF().bracket(
            acquire.fix1(EitherT::narrowK).value(),
            either -> either.fold(
                error -> acquireRecover(error),
                value -> use.andThen(EitherT::narrowK).apply(value).value()),
            either -> either.fold(cons(unit()), release.asFunction()));
    return EitherT.of(monadF(), bracket);
  }
}

@Instance
interface EitherTMonadDeferFromMonad<F extends Kind>
    extends EitherTMonadThrowFromMonad<F>,
            EitherTDefer<F, Throwable>,
            EitherTBracket<F>,
            MonadDefer<Higher1<Higher1<EitherT.µ, F>, Throwable>> { }

@Instance
interface EitherTMonadDeferFromMonadThrow<F extends Kind>
    extends EitherTMonadThrowFromMonadThrow<F>,
            EitherTDefer<F, Throwable>,
            EitherTBracket<F>,
            MonadDefer<Higher1<Higher1<EitherT.µ, F>, Throwable>> { }
