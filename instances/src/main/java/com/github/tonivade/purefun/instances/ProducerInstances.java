/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Producer_;
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
  default <T, R> Higher1<Producer_, R> map(Higher1<Producer_, T> value, Function1<T, R> mapper) {
    return value.fix1(Producer_::<T>narrowK).map(mapper);
  }
}

interface ProducerPure extends Applicative<Producer_> {

  @Override
  default <T> Higher1<Producer_, T> pure(T value) {
    return Producer.cons(value);
  }
}

interface ProducerApplicative extends ProducerPure {

  ProducerApplicative INSTANCE = new ProducerApplicative() {};

  @Override
  default <T, R> Higher1<Producer_, R> ap(Higher1<Producer_, T> value, Higher1<Producer_, Function1<T, R>> apply) {
    return Producer_.narrowK(value).flatMap(t -> Producer_.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface ProducerMonad extends ProducerPure, Monad<Producer_> {

  ProducerMonad INSTANCE = new ProducerMonad() {};

  @Override
  default <T, R> Higher1<Producer_, R> flatMap(Higher1<Producer_, T> value, Function1<T, ? extends Higher1<Producer_, R>> mapper) {
    return value.fix1(Producer_::narrowK).flatMap(mapper.andThen(Producer_::narrowK));
  }
}

interface ProducerComonad extends ProducerFunctor, Comonad<Producer_> {

  ProducerComonad INSTANCE = new ProducerComonad() {};

  @Override
  default <A, B> Higher1<Producer_, B> coflatMap(Higher1<Producer_, A> value, Function1<Higher1<Producer_, A>, B> map) {
    return Producer.cons(map.apply(value));
  }

  @Override
  default <A> A extract(Higher1<Producer_, A> value) {
    return value.fix1(Producer_::narrowK).get();
  }
}