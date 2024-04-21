/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.transformer.KleisliOf.toKleisli;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.transformer.Kleisli;
import com.github.tonivade.purefun.transformer.KleisliOf;
import com.github.tonivade.purefun.transformer.Kleisli_;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadReader;
import com.github.tonivade.purefun.typeclasses.MonadState;

public interface KleisliInstances {

  static <F extends Witness, Z> Monad<Kind<Kind<Kleisli_, F>, Z>> monad(Monad<F> monadF) {
    return KleisliMonad.instance(checkNonNull(monadF));
  }

  static <F extends Witness, Z, E> MonadError<Kind<Kind<Kleisli_, F>, Z>, E> monadError(MonadError<F, E> monadErrorF) {
    return KleisliMonadError.instance(monadErrorF);
  }

  static <F extends Witness, Z> MonadReader<Kind<Kind<Kleisli_, F>, Z>, Z> monadReader(Monad<F> monadF) {
    return KleisliMonadReader.instance(monadF);
  }

  static <F extends Witness, Z, S> MonadState<Kind<Kind<Kleisli_, F>, Z>, S> monadState(MonadState<F, S> monadStateF) {
    return KleisliMonadState.instance(monadStateF);
  }
}

interface KleisliMonad<F extends Witness, Z> extends Monad<Kind<Kind<Kleisli_, F>, Z>> {

  static <F extends Witness, Z> KleisliMonad<F, Z> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> Kleisli<F, Z, T> pure(T value) {
    return Kleisli.pure(monadF(), value);
  }

  @Override
  default <T, R> Kleisli<F, Z, R> flatMap(Kind<Kind<Kind<Kleisli_, F>, Z>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<Kind<Kleisli_, F>, Z>, ? extends R>> map) {
    return value.fix(toKleisli()).flatMap(map.andThen(KleisliOf::narrowK));
  }
}

interface KleisliMonadError<F extends Witness, R, E> extends MonadError<Kind<Kind<Kleisli_, F>, R>, E>, KleisliMonad<F, R> {

  static <F extends Witness, R, E> KleisliMonadError<F, R, E> instance(MonadError<F, E> monadErrorF) {
    return () -> monadErrorF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> Kleisli<F, R, A> raiseError(E error) {
    return Kleisli.of(monadF(), reader -> monadF().raiseError(error));
  }

  @Override
  default <A> Kleisli<F, R, A> handleErrorWith(
      Kind<Kind<Kind<Kleisli_, F>, R>, A> value,
      Function1<? super E, ? extends Kind<Kind<Kind<Kleisli_, F>, R>, ? extends A>> handler) {
    Kleisli<F, R, A> kleisli = value.fix(KleisliOf::narrowK);
    return Kleisli.of(monadF(),
        reader -> monadF().handleErrorWith(kleisli.run(reader),
            error -> handler.apply(error).fix(KleisliOf::narrowK).run(reader)));
  }
}

interface KleisliMonadReader<F extends Witness, R> extends MonadReader<Kind<Kind<Kleisli_, F>, R>, R>, KleisliMonad<F, R> {

  static <F extends Witness, Z> KleisliMonadReader<F, Z> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default Kleisli<F, R, R> ask() {
    return Kleisli.env(monadF());
  }
}

interface KleisliMonadState<F extends Witness, R, S> extends MonadState<Kind<Kind<Kleisli_, F>, R>, S>, KleisliMonad<F, R> {

  static <F extends Witness, R, S> KleisliMonadState<F, R, S> instance(MonadState<F, S> monadStateF) {
    return () -> monadStateF;
  }

  @Override
  MonadState<F, S> monadF();

  @Override
  default Kleisli<F, R, Unit> set(S state) {
    return Kleisli.of(monadF(), reader -> monadF().set(state));
  }

  @Override
  default Kleisli<F, R, S> get() {
    return Kleisli.of(monadF(), reader -> monadF().get());
  }
}
