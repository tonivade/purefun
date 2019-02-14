/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Eval.µ;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface EvalInstances {

  static Functor<Eval.µ> functor() {
    return new EvalFunctor() {};
  }
  
  static Applicative<Eval.µ> applicative() {
    return new EvalApplicative() {};
  }
  
  static Monad<Eval.µ> monad() {
    return new EvalMonad() {};
  }
  
  static Comonad<Eval.µ> comonad() {
    return new EvalComonad() {};
  }
  
  static Defer<Eval.µ> defer() {
    return new EvalDefer() {};
  }
}

interface EvalFunctor extends Functor<Eval.µ> {

  @Override
  default <T, R> Eval<R> map(Higher1<Eval.µ, T> value, Function1<T, R> mapper) {
    return Eval.narrowK(value).map(mapper);
  }
}

interface EvalPure extends Applicative<Eval.µ> {

  @Override
  default <T> Higher1<µ, T> pure(T value) {
    return Eval.now(value);
  }
}

interface EvalApplicative extends EvalPure {

  @Override
  default <T, R> Higher1<µ, R> ap(Higher1<µ, T> value, Higher1<µ, Function1<T, R>> apply) {
    return Eval.narrowK(value).flatMap(t -> Eval.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface EvalMonad extends EvalPure, Monad<Eval.µ> {

  @Override
  default <T, R> Eval<R> flatMap(Higher1<Eval.µ, T> value, Function1<T, ? extends Higher1<Eval.µ, R>> map) {
    return Eval.narrowK(value).flatMap(map.andThen(Eval::narrowK));
  }
}

interface EvalComonad extends EvalFunctor, Comonad<Eval.µ> {

  @Override
  default <A, B> Eval<B> coflatMap(Higher1<Eval.µ, A> value, Function1<Higher1<Eval.µ, A>, B> map) {
    return Eval.later(() -> map.apply(value));
  }
  
  @Override
  default <A> A extract(Higher1<Eval.µ, A> value) {
    return Eval.narrowK(value).value();
  }
}

interface EvalDefer extends Defer<Eval.µ> {

  @Override
  default <A> Higher1<µ, A> defer(Producer<Higher1<Eval.µ, A>> defer) {
    return Eval.defer(defer.andThen(Eval::narrowK));
  }
}