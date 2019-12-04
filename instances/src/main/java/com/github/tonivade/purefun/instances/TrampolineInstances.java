/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.free.Trampoline;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface TrampolineInstances {

  static Functor<Trampoline.µ> functor() {
    return TrampolineFunctor.INSTANCE;
  }

  static Applicative<Trampoline.µ> applicative() {
    return TrampolineApplicative.INSTANCE;
  }

  static Monad<Trampoline.µ> monad() {
    return TrampolineMonad.INSTANCE;
  }

  static Defer<Trampoline.µ> defer() {
    return TrampolineDefer.INSTANCE;
  }
}

@Instance
interface TrampolineFunctor extends Functor<Trampoline.µ> {

  TrampolineFunctor INSTANCE = new TrampolineFunctor() { };

  @Override
  default <T, R> Higher1<Trampoline.µ, R> map(Higher1<Trampoline.µ, T> value, Function1<T, R> mapper) {
    return Trampoline.narrowK(value).map(mapper).kind1();
  }
}

interface TrampolinePure extends Applicative<Trampoline.µ> {

  @Override
  default <T> Higher1<Trampoline.µ, T> pure(T value) {
    return Trampoline.done(value).kind1();
  }
}

@Instance
interface TrampolineApplicative extends TrampolinePure {

  TrampolineApplicative INSTANCE = new TrampolineApplicative() { };

  @Override
  default <T, R> Higher1<Trampoline.µ, R> ap(Higher1<Trampoline.µ, T> value, Higher1<Trampoline.µ, Function1<T, R>> apply) {
    return Trampoline.narrowK(value).flatMap(t -> Trampoline.narrowK(apply).map(f -> f.apply(t))).kind1();
  }
}

@Instance
interface TrampolineMonad extends TrampolinePure, Monad<Trampoline.µ> {

  TrampolineMonad INSTANCE = new TrampolineMonad() { };

  @Override
  default <T, R> Higher1<Trampoline.µ, R> flatMap(Higher1<Trampoline.µ, T> value,
      Function1<T, ? extends Higher1<Trampoline.µ, R>> map) {
    return Trampoline.narrowK(value).flatMap(map.andThen(Trampoline::narrowK)).kind1();
  }
}

@Instance
interface TrampolineDefer extends Defer<Trampoline.µ> {

  TrampolineDefer INSTANCE = new TrampolineDefer() { };

  @Override
  default <A> Higher1<Trampoline.µ, A> defer(Producer<Higher1<Trampoline.µ, A>> defer) {
    return Trampoline.more(() -> defer.get().fix1(Trampoline::narrowK)).kind1();
  }
}
