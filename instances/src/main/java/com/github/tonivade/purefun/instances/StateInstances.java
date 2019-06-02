/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.monad.State;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface StateInstances {

  static <S> Monad<Higher1<State.µ, S>> monad() {
    return new StateMonad<S>() { };
  }
}

interface StateMonad<S> extends Monad<Higher1<State.µ, S>> {

  @Override
  default <T> State<S, T> pure(T value) {
    return State.pure(value);
  }

  @Override
  default <T, R> State<S, R> flatMap(Higher1<Higher1<State.µ, S>, T> value,
      Function1<T, ? extends Higher1<Higher1<State.µ, S>, R>> map) {
    return State.narrowK(value).flatMap(map.andThen(State::narrowK));
  }
}
