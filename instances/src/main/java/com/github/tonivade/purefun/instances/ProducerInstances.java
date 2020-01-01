/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface ProducerInstances {

  static Functor<Producer.µ> functor() {
    return ProducerFunctor.instance();
  }

  static Applicative<Producer.µ> applicative() {
    return ProducerApplicative.instance();
  }

  static Monad<Producer.µ> monad() {
    return ProducerMonad.instance();
  }

  static Comonad<Producer.µ> comonad() {
    return ProducerComonad.instance();
  }
}

@Instance
interface ProducerFunctor extends Functor<Producer.µ> {

  @Override
  default <T, R> Higher1<Producer.µ, R> map(Higher1<Producer.µ, T> value, Function1<T, R> mapper) {
    return value.fix1(Producer::<T>narrowK).map(mapper).kind1();
  }
}

interface ProducerPure extends Applicative<Producer.µ> {

  @Override
  default <T> Higher1<Producer.µ, T> pure(T value) {
    return Producer.cons(value).kind1();
  }
}

@Instance
interface ProducerApplicative extends ProducerPure {

  @Override
  default <T, R> Higher1<Producer.µ, R> ap(Higher1<Producer.µ, T> value, Higher1<Producer.µ, Function1<T, R>> apply) {
    return Producer.narrowK(value).flatMap(t -> Producer.narrowK(apply).map(f -> f.apply(t))).kind1();
  }
}

@Instance
interface ProducerMonad extends ProducerPure, Monad<Producer.µ> {

  @Override
  default <T, R> Higher1<Producer.µ, R> flatMap(Higher1<Producer.µ, T> value, Function1<T, ? extends Higher1<Producer.µ, R>> mapper) {
    return value.fix1(Producer::narrowK).flatMap(mapper.andThen(Producer::narrowK)).kind1();
  }
}

@Instance
interface ProducerComonad extends ProducerFunctor, Comonad<Producer.µ> {

  @Override
  default <A, B> Higher1<Producer.µ, B> coflatMap(Higher1<Producer.µ, A> value, Function1<Higher1<Producer.µ, A>, B> map) {
    return Producer.cons(map.apply(value)).kind1();
  }

  @Override
  default <A> A extract(Higher1<Producer.µ, A> value) {
    return value.fix1(Producer::narrowK).get();
  }
}