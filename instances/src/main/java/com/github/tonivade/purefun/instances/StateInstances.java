/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Unit.unit;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.monad.State_;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadState;

public interface StateInstances {

  static <S> Monad<Higher1<State_, S>> monad() {
    return StateMonad.instance();
  }

  static <S> MonadState<Higher1<State_, S>, S> monadState() {
    return StateMonadState.instance();
  }

  static Console<Higher1<State_, ImmutableList<String>>> console() {
    return ConsoleState.INSTANCE;
  }
}

@Instance
interface StateMonad<S> extends Monad<Higher1<State_, S>> {

  @Override
  default <T> Higher2<State_, S, T> pure(T value) {
    return State.<S, T>pure(value).kind2();
  }

  @Override
  default <T, R> Higher2<State_, S, R> flatMap(Higher1<Higher1<State_, S>, T> value,
      Function1<T, ? extends Higher1<Higher1<State_, S>, R>> map) {
    return State_.narrowK(value).flatMap(map.andThen(State_::narrowK)).kind2();
  }
}

@Instance
interface StateMonadState<S> extends MonadState<Higher1<State_, S>, S>, StateMonad<S> {

  @Override
  default Higher2<State_, S, S> get() {
    return State.<S>get().kind2();
  }

  @Override
  default Higher2<State_, S, Unit> set(S state) {
    return State.set(state).kind2();
  }
}

final class ConsoleState implements Console<Higher1<State_, ImmutableList<String>>> {

  protected static final ConsoleState INSTANCE = new ConsoleState();

  @Override
  public Higher2<State_, ImmutableList<String>, String> readln() {
    return State.<ImmutableList<String>, String>state(list -> Tuple.of(list.tail(), list.head().get())).kind2();
  }

  @Override
  public Higher2<State_, ImmutableList<String>, Unit> println(String text) {
    return State.<ImmutableList<String>, Unit>state(list -> Tuple.of(list.append(text), unit())).kind2();
  }
}