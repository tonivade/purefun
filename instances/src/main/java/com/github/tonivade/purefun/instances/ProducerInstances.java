/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface ProducerInstances {

  static Functor<Producer.µ> functor() {
    return new ProducerFunctor() {};
  }

  static Applicative<Producer.µ> applicative() {
    return new ProducerApplicative() {};
  }

  static Monad<Producer.µ> monad() {
    return new ProducerMonad() {};
  }
}

@Instance
interface ProducerFunctor extends Functor<Producer.µ> {

  @Override
  default <T, R> Producer<R> map(Higher1<Producer.µ, T> value, Function1<T, R> mapper) {
    return value.fix1(Producer::narrowK).map(mapper);
  }
}

interface ProducerPure extends Applicative<Producer.µ> {

  @Override
  default <T> Producer<T> pure(T value) {
    return Producer.cons(value);
  }
}

@Instance
interface ProducerApplicative extends ProducerPure {

  @Override
  default <T, R> Producer<R> ap(Higher1<Producer.µ, T> value, Higher1<Producer.µ, Function1<T, R>> apply) {
    return Producer.narrowK(value).flatMap(t -> Producer.narrowK(apply).map(f -> f.apply(t)));
  }
}

@Instance
interface ProducerMonad extends ProducerPure, Monad<Producer.µ> {

  @Override
  default <T, R> Producer<R> flatMap(Higher1<Producer.µ, T> value, Function1<T, ? extends Higher1<Producer.µ, R>> mapper) {
    return value.fix1(Producer::narrowK).flatMap(mapper.andThen(Producer::narrowK));
  }
}