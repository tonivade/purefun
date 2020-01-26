/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.transformer.StateT;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadState;

public interface StateTInstances {

  static <F extends Kind, S> Monad<Higher1<Higher1<StateT.µ, F>, S>> monad(Monad<F> monadF) {
    return StateTMonad.instance(requireNonNull(monadF));
  }

  static <F extends Kind, S, E> MonadError<Higher1<Higher1<StateT.µ, F>, S>, E> monadError(MonadError<F, E> monadErrorF) {
    return StateTMonadError.instance(requireNonNull(monadErrorF));
  }

  static <F extends Kind, S> MonadState<Higher1<Higher1<StateT.µ, F>, S>, S> monadState(Monad<F> monadF) {
    return StateTMonadState.instance(requireNonNull(monadF));
  }
}

interface StateTMonad<F extends Kind, S> extends Monad<Higher1<Higher1<StateT.µ, F>, S>> {

  static <F extends Kind, S> StateTMonad<F, S> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> Higher3<StateT.µ, F, S, T> pure(T value) {
    return StateT.<F, S, T>pure(monadF(), value).kind3();
  }

  @Override
  default <T, R> Higher3<StateT.µ, F, S, R> flatMap(Higher1<Higher1<Higher1<StateT.µ, F>, S>, T> value,
      Function1<T, ? extends Higher1<Higher1<Higher1<StateT.µ, F>, S>, R>> map) {
    return StateT.narrowK(value).flatMap(map.andThen(StateT::narrowK)).kind3();
  }
}

interface StateTMonadError<F extends Kind, S, E> extends MonadError<Higher1<Higher1<StateT.µ, F>, S>, E>, StateTMonad<F, S> {

  static <F extends Kind, S, E> StateTMonadError<F, S, E> instance(MonadError<F, E> monadErrorF) {
    return () -> monadErrorF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> Higher3<StateT.µ, F, S, A> raiseError(E error) {
    Higher1<F, A> raiseError = monadF().raiseError(error);
    return StateT.<F, S, A>state(monadF(), state -> monadF().map(raiseError, value -> Tuple.of(state, value))).kind3();
  }

  @Override
  default <A> Higher3<StateT.µ, F, S, A> handleErrorWith(
      Higher1<Higher1<Higher1<StateT.µ, F>, S>, A> value,
      Function1<E, ? extends Higher1<Higher1<Higher1<StateT.µ, F>, S>, A>> handler) {
    StateT<F, S, A> stateT = value.fix1(StateT::narrowK);
    return StateT.<F, S, A>state(monadF(),
        state -> monadF().handleErrorWith(stateT.run(state),
            error -> handler.apply(error).fix1(StateT::narrowK).run(state))).kind3();
  }
}

interface StateTMonadState<F extends Kind, S> extends MonadState<Higher1<Higher1<StateT.µ, F>, S>, S>, StateTMonad<F, S> {

  static <F extends Kind, S> StateTMonadState<F, S> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default Higher3<StateT.µ, F, S, S> get() {
    return StateT.<F, S>get(monadF()).kind3();
  }

  @Override
  default Higher3<StateT.µ, F, S, Unit> set(S state) {
    return StateT.set(monadF(), state).kind3();
  }
}
