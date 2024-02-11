/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.ProducerOf;
import com.github.tonivade.purefun.core.Producer_;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface ProducerInstances {

  static Functor<Producer_> functor() {
    return ProducerFunctor.INSTANCE;
  }

  static Applicative<Producer_> applicative() {
    return ProducerApplicative.INSTANCE;
  }

  static Monad<Producer_> monad() {
    return ProducerMonad.INSTANCE;
  }

  static Comonad<Producer_> comonad() {
    return ProducerComonad.INSTANCE;
  }
}

interface ProducerFunctor extends Functor<Producer_> {

  ProducerFunctor INSTANCE = new ProducerFunctor() {};

  @Override
  default <T, R> Kind<Producer_, R> map(Kind<Producer_, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return value.fix(ProducerOf::<T>narrowK).map(mapper);
  }
}

interface ProducerPure extends Applicative<Producer_> {

  @Override
  default <T> Kind<Producer_, T> pure(T value) {
    return Producer.cons(value);
  }
}

interface ProducerApplicative extends ProducerPure {

  ProducerApplicative INSTANCE = new ProducerApplicative() {};

  @Override
  default <T, R> Kind<Producer_, R> ap(Kind<Producer_, ? extends T> value,
      Kind<Producer_, ? extends Function1<? super T, ? extends R>> apply) {
    return ProducerOf.narrowK(value).flatMap(t -> ProducerOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface ProducerMonad extends ProducerPure, Monad<Producer_> {

  ProducerMonad INSTANCE = new ProducerMonad() {};

  @Override
  default <T, R> Kind<Producer_, R> flatMap(Kind<Producer_, ? extends T> value, Function1<? super T, ? extends Kind<Producer_, ? extends R>> mapper) {
    return value.fix(ProducerOf::narrowK).flatMap(mapper.andThen(ProducerOf::narrowK));
  }
}

interface ProducerComonad extends ProducerFunctor, Comonad<Producer_> {

  ProducerComonad INSTANCE = new ProducerComonad() {};

  @Override
  default <A, B> Kind<Producer_, B> coflatMap(Kind<Producer_, ? extends A> value, Function1<? super Kind<Producer_, ? extends A>, ? extends B> map) {
    return Producer.cons(map.apply(value));
  }

  @Override
  default <A> A extract(Kind<Producer_, ? extends A> value) {
    return value.fix(ProducerOf::narrowK).get();
  }
}