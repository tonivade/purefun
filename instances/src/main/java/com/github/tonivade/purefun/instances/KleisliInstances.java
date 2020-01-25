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
import com.github.tonivade.purefun.transformer.Kleisli;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadReader;

public interface KleisliInstances {

  static <F extends Kind, Z> Monad<Higher1<Higher1<Kleisli.µ, F>, Z>> monad(Monad<F> monadF) {
    return KleisliMonad.instance(requireNonNull(monadF));
  }

  static <F extends Kind, Z> MonadReader<Higher1<Higher1<Kleisli.µ, F>, Z>, Z> monadReader(Monad<F> monadF) {
    return KleisliMonadReader.instance(monadF);
  }
}

interface KleisliMonad<F extends Kind, Z> extends Monad<Higher1<Higher1<Kleisli.µ, F>, Z>> {

  static <F extends Kind, Z> KleisliMonad<F, Z> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> Higher3<Kleisli.µ, F, Z, T> pure(T value) {
    return Kleisli.<F, Z, T>pure(monadF(), value).kind3();
  }

  @Override
  default <T, R> Higher3<Kleisli.µ, F, Z, R> flatMap(Higher1<Higher1<Higher1<Kleisli.µ, F>, Z>, T> value,
      Function1<T, ? extends Higher1<Higher1<Higher1<Kleisli.µ, F>, Z>, R>> map) {
    return Kleisli.narrowK(value).flatMap(map.andThen(Kleisli::narrowK)).kind3();
  }
}

interface KleisliMonadReader<F extends Kind, R> extends MonadReader<Higher1<Higher1<Kleisli.µ, F>, R>, R>, KleisliMonad<F, R> {

  static <F extends Kind, Z> KleisliMonadReader<F, Z> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default Higher3<Kleisli.µ, F, R, R> ask() {
    return Kleisli.<F, R>env(monadF()).kind3();
  }
}