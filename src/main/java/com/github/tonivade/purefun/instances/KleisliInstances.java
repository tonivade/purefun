/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.monad.Kleisli;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface KleisliInstances {
  static <F extends Kind, Z> Monad<Higher1<Higher1<Kleisli.µ, F>, Z>> monad(Monad<F> monadF) {
    requireNonNull(monadF);
    return new KleisliMonad<F, Z>() {
      
      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }
}

interface KleisliMonad<F extends Kind, Z> extends Monad<Higher1<Higher1<Kleisli.µ, F>, Z>> {

  Monad<F> monadF();
  
  @Override
  default <T> Kleisli<F, Z, T> pure(T value) {
    return Kleisli.pure(monadF(), value);
  }

  @Override
  default <T, R> Kleisli<F, Z, R> flatMap(Higher1<Higher1<Higher1<Kleisli.µ, F>, Z>, T> value,
      Function1<T, ? extends Higher1<Higher1<Higher1<Kleisli.µ, F>, Z>, R>> map) {
    return Kleisli.narrowK(value).flatMap(map.andThen(Kleisli::narrowK));
  }
}
