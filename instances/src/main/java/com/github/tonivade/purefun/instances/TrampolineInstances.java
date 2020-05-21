/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.free.Trampoline;
import com.github.tonivade.purefun.free.TrampolineOf;
import com.github.tonivade.purefun.free.Trampoline_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface TrampolineInstances {

  static Functor<Trampoline_> functor() {
    return TrampolineFunctor.INSTANCE;
  }

  static Applicative<Trampoline_> applicative() {
    return TrampolineApplicative.INSTANCE;
  }

  static Monad<Trampoline_> monad() {
    return TrampolineMonad.INSTANCE;
  }

  static Defer<Trampoline_> defer() {
    return TrampolineDefer.INSTANCE;
  }
}

interface TrampolineFunctor extends Functor<Trampoline_> {

  TrampolineFunctor INSTANCE = new TrampolineFunctor() {};

  @Override
  default <T, R> Kind<Trampoline_, R> map(Kind<Trampoline_, T> value, Function1<T, R> mapper) {
    return TrampolineOf.narrowK(value).map(mapper);
  }
}

interface TrampolinePure extends Applicative<Trampoline_> {

  @Override
  default <T> Kind<Trampoline_, T> pure(T value) {
    return Trampoline.done(value);
  }
}

interface TrampolineApplicative extends TrampolinePure {

  TrampolineApplicative INSTANCE = new TrampolineApplicative() {};

  @Override
  default <T, R> Kind<Trampoline_, R> ap(Kind<Trampoline_, T> value, Kind<Trampoline_, Function1<T, R>> apply) {
    return TrampolineOf.narrowK(value).flatMap(t -> TrampolineOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface TrampolineMonad extends TrampolinePure, Monad<Trampoline_> {

  TrampolineMonad INSTANCE = new TrampolineMonad() {};

  @Override
  default <T, R> Kind<Trampoline_, R> flatMap(Kind<Trampoline_, T> value,
      Function1<T, ? extends Kind<Trampoline_, R>> map) {
    return TrampolineOf.narrowK(value).flatMap(map.andThen(TrampolineOf::narrowK));
  }
}

interface TrampolineDefer extends Defer<Trampoline_> {

  TrampolineDefer INSTANCE = new TrampolineDefer() {};

  @Override
  default <A> Kind<Trampoline_, A> defer(Producer<Kind<Trampoline_, A>> defer) {
    return Trampoline.more(() -> defer.get().fix(TrampolineOf::narrowK));
  }
}
