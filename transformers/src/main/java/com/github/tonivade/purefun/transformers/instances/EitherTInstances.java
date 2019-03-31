/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformers.instances;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.transformers.EitherT;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;

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

  static <F extends Kind, L> Defer<Higher1<Higher1<EitherT.µ, F>, L>> defer(Monad<F> monadF, Defer<F> deferF) {
    requireNonNull(monadF);
    requireNonNull(deferF);
    return new EitherTDefer<F, L>() {

      @Override
      public Monad<F> monadF() { return monadF; }

      @Override
      public Defer<F> deferF() { return deferF; }
    };
  }

}

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
            either -> either.fold(e -> handler.andThen(EitherT::narrowK).apply(e).value(),
                a -> monadF().pure(Either.right(a)))));
  }
}

interface EitherTMonadErrorFromMonadError<F extends Kind, E>
    extends MonadError<Higher1<Higher1<EitherT.µ, F>, E>, E>, EitherTMonad<F, E> {

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> EitherT<F, E, A> raiseError(E error) {
    return EitherT.of(monadF(), monadF().raiseError(error));
  }

  @Override
  default <A> EitherT<F, E, A> handleErrorWith(Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A> value,
      Function1<E, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A>> handler) {
    return EitherT.of(monadF(), monadF().handleErrorWith(EitherT.narrowK(value).value(),
        error -> handler.andThen(EitherT::narrowK).apply(error).value()));
  }
}

interface EitherTDefer<F extends Kind, E> extends Defer<Higher1<Higher1<EitherT.µ, F>, E>> {

  Monad<F> monadF();
  Defer<F> deferF();

  @Override
  default <A> EitherT<F, E, A> defer(Producer<Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A>> defer) {
    return EitherT.of(monadF(), deferF().defer(() -> defer.andThen(EitherT::narrowK).get().value()));
  }
}
