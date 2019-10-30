/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.transformer.StateT;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface StateTInstances {

  static <F extends Kind, S> Monad<Higher1<Higher1<StateT.µ, F>, S>> monad(Monad<F> monadF) {
    return StateTMonad.instance(requireNonNull(monadF));
  }
}

@Instance
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
