/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Unit.unit;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.monad.StateOf;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadState;

@SuppressWarnings("unchecked")
public interface StateInstances {

  static <S> Monad<State<S, ?>> monad() {
    return StateMonad.INSTANCE;
  }

  static <S> MonadState<State<S, ?>, S> monadState() {
    return StateMonadState.INSTANCE;
  }

  static Console<State<ImmutableList<String>, ?>> console() {
    return StateConsole.INSTANCE;
  }
}

interface StateMonad<S> extends Monad<State<S, ?>> {

  @SuppressWarnings("rawtypes")
  StateMonad INSTANCE = new StateMonad() {};

  @Override
  default <T> State<S, T> pure(T value) {
    return State.pure(value);
  }

  @Override
  default <T, R> State<S, R> flatMap(Kind<State<S, ?>, ? extends T> value,
      Function1<? super T, ? extends Kind<State<S, ?>, ? extends R>> map) {
    return StateOf.toState(value).flatMap(map.andThen(StateOf::toState));
  }
}

interface StateMonadState<S> extends MonadState<State<S, ?>, S>, StateMonad<S> {

  @SuppressWarnings("rawtypes")
  StateMonadState INSTANCE = new StateMonadState() {};

  @Override
  default State<S, S> get() {
    return State.get();
  }

  @Override
  default State<S, Unit> set(S state) {
    return State.set(state);
  }
}

final class StateConsole implements Console<State<ImmutableList<String>, ?>> {

  static final StateConsole INSTANCE = new StateConsole();

  @Override
  public State<ImmutableList<String>, String> readln() {
    return State.state(list -> Tuple.of(list.tail(), list.head().getOrElseThrow()));
  }

  @Override
  public State<ImmutableList<String>, Unit> println(String text) {
    return State.state(list -> Tuple.of(list.append(text), unit()));
  }
}