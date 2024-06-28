/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Trampoline;
import com.github.tonivade.purefun.core.TrampolineOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface TrampolineInstances {

  static Functor<Trampoline<?>> functor() {
    return TrampolineFunctor.INSTANCE;
  }

  static Applicative<Trampoline<?>> applicative() {
    return TrampolineApplicative.INSTANCE;
  }

  static Monad<Trampoline<?>> monad() {
    return TrampolineMonad.INSTANCE;
  }

  static Defer<Trampoline<?>> defer() {
    return TrampolineDefer.INSTANCE;
  }
}

interface TrampolineFunctor extends Functor<Trampoline<?>> {

  TrampolineFunctor INSTANCE = new TrampolineFunctor() {};

  @Override
  default <T, R> Kind<Trampoline<?>, R> map(
      Kind<Trampoline<?>, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return TrampolineOf.toTrampoline(value).map(mapper);
  }
}

interface TrampolinePure extends Applicative<Trampoline<?>> {

  @Override
  default <T> Kind<Trampoline<?>, T> pure(T value) {
    return Trampoline.done(value);
  }
}

interface TrampolineApplicative extends TrampolinePure {

  TrampolineApplicative INSTANCE = new TrampolineApplicative() {};

  @Override
  default <T, R> Kind<Trampoline<?>, R> ap(Kind<Trampoline<?>, ? extends T> value,
      Kind<Trampoline<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return TrampolineOf.toTrampoline(value).flatMap(t -> TrampolineOf.toTrampoline(apply).map(f -> f.apply(t)));
  }
}

interface TrampolineMonad extends TrampolinePure, Monad<Trampoline<?>> {

  TrampolineMonad INSTANCE = new TrampolineMonad() {};

  @Override
  default <T, R> Kind<Trampoline<?>, R> flatMap(Kind<Trampoline<?>, ? extends T> value,
      Function1<? super T, ? extends Kind<Trampoline<?>, ? extends R>> map) {
    return TrampolineOf.toTrampoline(value).flatMap(map.andThen(TrampolineOf::toTrampoline));
  }
}

interface TrampolineDefer extends Defer<Trampoline<?>> {

  TrampolineDefer INSTANCE = new TrampolineDefer() {};

  @Override
  default <A> Kind<Trampoline<?>, A> defer(Producer<? extends Kind<Trampoline<?>, ? extends A>> defer) {
    return Trampoline.more(() -> {
      Kind<Trampoline<?>, A> value = Kind.narrowK(defer.get());
      return value.fix(TrampolineOf::toTrampoline);
    });
  }
}
