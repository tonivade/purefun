/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.ProducerOf;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface ProducerInstances {

  static Functor<Producer<?>> functor() {
    return ProducerFunctor.INSTANCE;
  }

  static Applicative<Producer<?>> applicative() {
    return ProducerApplicative.INSTANCE;
  }

  static Monad<Producer<?>> monad() {
    return ProducerMonad.INSTANCE;
  }

  static Comonad<Producer<?>> comonad() {
    return ProducerComonad.INSTANCE;
  }
}

interface ProducerFunctor extends Functor<Producer<?>> {

  ProducerFunctor INSTANCE = new ProducerFunctor() {};

  @Override
  default <T, R> Kind<Producer<?>, R> map(Kind<Producer<?>, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return value.<Producer<T>>fix().map(mapper);
  }
}

interface ProducerPure extends Applicative<Producer<?>> {

  @Override
  default <T> Kind<Producer<?>, T> pure(T value) {
    return Producer.cons(value);
  }
}

interface ProducerApplicative extends ProducerPure {

  ProducerApplicative INSTANCE = new ProducerApplicative() {};

  @Override
  default <T, R> Kind<Producer<?>, R> ap(Kind<Producer<?>, ? extends T> value,
      Kind<Producer<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return value.<Producer<T>>fix().flatMap(t -> apply.<Producer<Function1<T, R>>>fix().map(f -> f.apply(t)));
  }
}

interface ProducerMonad extends ProducerPure, Monad<Producer<?>> {

  ProducerMonad INSTANCE = new ProducerMonad() {};

  @Override
  default <T, R> Kind<Producer<?>, R> flatMap(Kind<Producer<?>, ? extends T> value, Function1<? super T, ? extends Kind<Producer<?>, ? extends R>> mapper) {
    return value.<Producer<T>>fix().flatMap(mapper.andThen(ProducerOf::toProducer));
  }
}

interface ProducerComonad extends ProducerFunctor, Comonad<Producer<?>> {

  ProducerComonad INSTANCE = new ProducerComonad() {};

  @Override
  default <A, B> Kind<Producer<?>, B> coflatMap(Kind<Producer<?>, ? extends A> value, Function1<? super Kind<Producer<?>, ? extends A>, ? extends B> map) {
    return Producer.cons(map.apply(value));
  }

  @Override
  default <A> A extract(Kind<Producer<?>, ? extends A> value) {
    return value.<Producer<A>>fix().get();
  }
}