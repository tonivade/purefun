/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.concurrent.PromiseOf.toPromise;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.Promise;
import com.github.tonivade.purefun.concurrent.PromiseOf;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface PromiseInstances {

  static Functor<Promise<?>> functor() {
    return PromiseFunctor.INSTANCE;
  }

  static Applicative<Promise<?>> applicative() {
    return applicative(Future.DEFAULT_EXECUTOR);
  }

  static Applicative<Promise<?>> applicative(Executor executor) {
    return PromiseApplicative.instance(checkNonNull(executor));
  }

  static Monad<Promise<?>> monad() {
    return monad(Future.DEFAULT_EXECUTOR);
  }

  static Monad<Promise<?>> monad(Executor executor) {
    return PromiseMonad.instance(checkNonNull(executor));
  }
}

interface PromiseFunctor extends Functor<Promise<?>> {

  PromiseFunctor INSTANCE = new PromiseFunctor() {};

  @Override
  default <T, R> Kind<Promise<?>, R> map(Kind<Promise<?>, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return value.fix(toPromise()).map(mapper);
  }
}

interface PromiseExecutorHolder {
  Executor executor();
}

interface PromisePure extends Applicative<Promise<?>>, PromiseExecutorHolder {

  @Override
  default <T> Kind<Promise<?>, T> pure(T value) {
    return Promise.<T>make(executor()).succeeded(value);
  }
}

interface PromiseApplicative extends PromisePure {

  static PromiseApplicative instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Kind<Promise<?>, R> ap(Kind<Promise<?>, ? extends T> value,
      Kind<Promise<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return value.fix(PromiseOf::<T>narrowK).ap(apply.fix(PromiseOf::narrowK));
  }
}

interface PromiseMonad extends PromisePure, Monad<Promise<?>> {

  static PromiseMonad instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Kind<Promise<?>, R> flatMap(Kind<Promise<?>, ? extends T> value,
      Function1<? super T, ? extends Kind<Promise<?>, ? extends R>> map) {
    return value.fix(toPromise()).flatMap(map.andThen(PromiseOf::narrowK));
  }

  /**
   * XXX In order to create real parallel computations, we need to override ap to use the
   * applicative version of the ap method
   */
  @Override
  default <T, R> Kind<Promise<?>, R> ap(Kind<Promise<?>, ? extends T> value,
      Kind<Promise<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return PromiseInstances.applicative(executor()).ap(value, apply);
  }
}
