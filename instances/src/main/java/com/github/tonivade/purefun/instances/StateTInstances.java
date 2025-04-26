/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.transformer.StateT;
import com.github.tonivade.purefun.transformer.StateTOf;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadReader;
import com.github.tonivade.purefun.typeclasses.MonadState;

public interface StateTInstances {

  static <F extends Kind<F, ?>, S> Monad<StateT<F, S, ?>> monad(Monad<F> monadF) {
    return StateTMonad.instance(checkNonNull(monadF));
  }

  static <F extends Kind<F, ?>, S> MonadState<StateT<F, S, ?>, S> monadState(Monad<F> monadF) {
    return StateTMonadState.instance(checkNonNull(monadF));
  }

  static <F extends Kind<F, ?>, S, E> MonadError<StateT<F, S, ?>, E> monadError(MonadError<F, E> monadErrorF) {
    return StateTMonadError.instance(checkNonNull(monadErrorF));
  }

  static <F extends Kind<F, ?>, S, R> MonadReader<StateT<F, S, ?>, R> monadReader(MonadReader<F, R> monadReaderF) {
    return StateTMonadReader.instance(checkNonNull(monadReaderF));
  }
}

interface StateTMonad<F extends Kind<F, ?>, S> extends Monad<StateT<F, S, ?>> {

  static <F extends Kind<F, ?>, S> StateTMonad<F, S> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> StateT<F, S, T> pure(T value) {
    return StateT.pure(monadF(), value);
  }

  @Override
  default <T, R> StateT<F, S, R> flatMap(Kind<StateT<F, S, ?>, ? extends T> value,
      Function1<? super T, ? extends Kind<StateT<F, S, ?>, ? extends R>> map) {
    return StateTOf.toStateT(value).flatMap(map.andThen(StateTOf::toStateT));
  }
}

interface StateTMonadError<F extends Kind<F, ?>, S, E> extends MonadError<StateT<F, S, ?>, E>, StateTMonad<F, S> {

  static <F extends Kind<F, ?>, S, E> StateTMonadError<F, S, E> instance(MonadError<F, E> monadErrorF) {
    return () -> monadErrorF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> StateT<F, S, A> raiseError(E error) {
    Kind<F, A> raiseError = monadF().raiseError(error);
    return StateT.state(monadF(), state -> monadF().map(raiseError, value -> Tuple.of(state, value)));
  }

  @Override
  default <A> StateT<F, S, A> handleErrorWith(
      Kind<StateT<F, S, ?>, A> value,
      Function1<? super E, ? extends Kind<StateT<F, S, ?>, ? extends A>> handler) {
    StateT<F, S, A> stateT = value.fix(StateTOf::toStateT);
    return StateT.state(monadF(),
        state -> monadF().handleErrorWith(stateT.run(state),
            error -> handler.apply(error).fix(StateTOf::<F, S, A>toStateT).run(state)));
  }
}

interface StateTMonadState<F extends Kind<F, ?>, S> extends MonadState<StateT<F, S, ?>, S>, StateTMonad<F, S> {

  static <F extends Kind<F, ?>, S> StateTMonadState<F, S> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default StateT<F, S, S> get() {
    return StateT.get(monadF());
  }

  @Override
  default StateT<F, S, Unit> set(S state) {
    return StateT.set(monadF(), state);
  }
}

interface StateTMonadReader<F extends Kind<F, ?>, S, R> extends MonadReader<StateT<F, S, ?>, R>, StateTMonad<F, S> {

  static <F extends Kind<F, ?>, S, R> StateTMonadReader<F, S, R> instance(MonadReader<F, R> monadReaderF) {
    return () -> monadReaderF;
  }

  @Override
  MonadReader<F, R> monadF();

  @Override
  default StateT<F, S, R> ask() {
    return StateT.state(monadF(), state -> monadF().map(monadF().ask(), reader -> Tuple.of(state, reader)));
  }
}
