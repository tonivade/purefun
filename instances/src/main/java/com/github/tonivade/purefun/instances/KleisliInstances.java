/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.transformer.Kleisli;
import com.github.tonivade.purefun.transformer.Kleisli_;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadReader;
import com.github.tonivade.purefun.typeclasses.MonadState;

public interface KleisliInstances {

  static <F extends Kind, Z> Monad<Higher1<Higher1<Kleisli_, F>, Z>> monad(Monad<F> monadF) {
    return KleisliMonad.instance(checkNonNull(monadF));
  }

  static <F extends Kind, Z, E> MonadError<Higher1<Higher1<Kleisli_, F>, Z>, E> monadError(MonadError<F, E> monadErrorF) {
    return KleisliMonadError.instance(monadErrorF);
  }

  static <F extends Kind, Z> MonadReader<Higher1<Higher1<Kleisli_, F>, Z>, Z> monadReader(Monad<F> monadF) {
    return KleisliMonadReader.instance(monadF);
  }

  static <F extends Kind, Z, S> MonadState<Higher1<Higher1<Kleisli_, F>, Z>, S> monadState(MonadState<F, S> monadStateF) {
    return KleisliMonadState.instance(monadStateF);
  }
}

interface KleisliMonad<F extends Kind, Z> extends Monad<Higher1<Higher1<Kleisli_, F>, Z>> {

  static <F extends Kind, Z> KleisliMonad<F, Z> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> Higher3<Kleisli_, F, Z, T> pure(T value) {
    return Kleisli.<F, Z, T>pure(monadF(), value);
  }

  @Override
  default <T, R> Higher3<Kleisli_, F, Z, R> flatMap(Higher1<Higher1<Higher1<Kleisli_, F>, Z>, T> value,
      Function1<T, ? extends Higher1<Higher1<Higher1<Kleisli_, F>, Z>, R>> map) {
    return Kleisli_.narrowK(value).flatMap(map.andThen(Kleisli_::narrowK));
  }
}

interface KleisliMonadError<F extends Kind, R, E> extends MonadError<Higher1<Higher1<Kleisli_, F>, R>, E>, KleisliMonad<F, R> {

  static <F extends Kind, R, E> KleisliMonadError<F, R, E> instance(MonadError<F, E> monadErrorF) {
    return () -> monadErrorF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> Higher3<Kleisli_, F, R, A> raiseError(E error) {
    return Kleisli.<F, R, A>of(monadF(), reader -> monadF().raiseError(error));
  }

  @Override
  default <A> Higher3<Kleisli_, F, R, A> handleErrorWith(
      Higher1<Higher1<Higher1<Kleisli_, F>, R>, A> value,
      Function1<E, ? extends Higher1<Higher1<Higher1<Kleisli_, F>, R>, A>> handler) {
    Kleisli<F, R, A> kleisli = value.fix1(Kleisli_::narrowK);
    return Kleisli.<F, R, A>of(monadF(),
        reader -> monadF().handleErrorWith(kleisli.run(reader),
            error -> handler.apply(error).fix1(Kleisli_::narrowK).run(reader)));
  }
}

interface KleisliMonadReader<F extends Kind, R> extends MonadReader<Higher1<Higher1<Kleisli_, F>, R>, R>, KleisliMonad<F, R> {

  static <F extends Kind, Z> KleisliMonadReader<F, Z> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default Higher3<Kleisli_, F, R, R> ask() {
    return Kleisli.<F, R>env(monadF());
  }
}

interface KleisliMonadState<F extends Kind, R, S> extends MonadState<Higher1<Higher1<Kleisli_, F>, R>, S>, KleisliMonad<F, R> {

  static <F extends Kind, R, S> KleisliMonadState<F, R, S> instance(MonadState<F, S> monadStateF) {
    return () -> monadStateF;
  }

  @Override
  MonadState<F, S> monadF();

  @Override
  default Higher3<Kleisli_, F, R, Unit> set(S state) {
    return Kleisli.<F, R, Unit>of(monadF(), reader -> monadF().set(state));
  }

  @Override
  default Higher3<Kleisli_, F, R, S> get() {
    return Kleisli.<F, R, S>of(monadF(), reader -> monadF().get());
  }
}
