/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.monad.Trampoline;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface TrampolineInstances {

  static Functor<Trampoline.µ> functor() {
    return new TrampolineFunctor() {};
  }

  static Applicative<Trampoline.µ> applicative() {
    return new TrampolineApplicative() {};
  }

  static Monad<Trampoline.µ> monad() {
    return new TrampolineMonad() {};
  }
}

interface TrampolineFunctor extends Functor<Trampoline.µ> {

  @Override
  default <T, R> Trampoline<R> map(Higher1<Trampoline.µ, T> value, Function1<T, R> mapper) {
    return Trampoline.narrowK(value).map(mapper);
  }
}

interface TrampolinePure extends Applicative<Trampoline.µ> {

  @Override
  default <T> Trampoline<T> pure(T value) {
    return Trampoline.done(value);
  }
}

interface TrampolineApplicative extends TrampolinePure {

  @Override
  default <T, R> Trampoline<R> ap(Higher1<Trampoline.µ, T> value, Higher1<Trampoline.µ, Function1<T, R>> apply) {
    return Trampoline.narrowK(value).flatMap(t -> Trampoline.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface TrampolineMonad extends TrampolinePure, Monad<Trampoline.µ> {

  @Override
  default <T, R> Trampoline<R> flatMap(Higher1<Trampoline.µ, T> value,
      Function1<T, ? extends Higher1<Trampoline.µ, R>> map) {
    return Trampoline.narrowK(value).flatMap(map);
  }
}
