/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.transformer.StateT;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface StateTInstances {

  static <F extends Kind, S> Monad<Higher1<Higher1<StateT.µ, F>, S>> monad(Monad<F> monadF) {
    requireNonNull(monadF);
    return new StateTMonad<F, S>() {

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }
}

interface StateTMonad<F extends Kind, S> extends Monad<Higher1<Higher1<StateT.µ, F>, S>> {

  Monad<F> monadF();

  @Override
  default <T> StateT<F, S, T> pure(T value) {
    return StateT.pure(monadF(), value);
  }

  @Override
  default <T, R> StateT<F, S, R> flatMap(Higher1<Higher1<Higher1<StateT.µ, F>, S>, T> value,
      Function1<T, ? extends Higher1<Higher1<Higher1<StateT.µ, F>, S>, R>> map) {
    return StateT.narrowK(value).flatMap(map.andThen(StateT::narrowK));
  }
}
