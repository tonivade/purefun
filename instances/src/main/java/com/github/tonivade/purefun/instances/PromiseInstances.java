/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.concurrent.PromiseOf.toPromise;

import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.Promise;
import com.github.tonivade.purefun.concurrent.PromiseOf;
import com.github.tonivade.purefun.concurrent.Promise_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface PromiseInstances {

  static Functor<Promise_> functor() {
    return PromiseFunctor.INSTANCE;
  }

  static Applicative<Promise_> applicative() {
    return applicative(Future.DEFAULT_EXECUTOR);
  }

  static Applicative<Promise_> applicative(Executor executor) {
    return PromiseApplicative.instance(checkNonNull(executor));
  }

  static Monad<Promise_> monad() {
    return monad(Future.DEFAULT_EXECUTOR);
  }

  static Monad<Promise_> monad(Executor executor) {
    return PromiseMonad.instance(checkNonNull(executor));
  }
}

interface PromiseFunctor extends Functor<Promise_> {

  PromiseFunctor INSTANCE = new PromiseFunctor() {};

  @Override
  default <T, R> Kind<Promise_, R> map(Kind<Promise_, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return value.fix(toPromise()).map(mapper);
  }
}

interface PromiseExecutorHolder {
  Executor executor();
}

interface PromisePure extends Applicative<Promise_>, PromiseExecutorHolder {

  @Override
  default <T> Kind<Promise_, T> pure(T value) {
    return Promise.<T>make(executor()).succeeded(value);
  }
}

interface PromiseApplicative extends PromisePure {

  static PromiseApplicative instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Kind<Promise_, R> ap(Kind<Promise_, ? extends T> value, 
      Kind<Promise_, ? extends Function1<? super T, ? extends R>> apply) {
    return value.fix(PromiseOf::<T>narrowK).ap(apply.fix(PromiseOf::narrowK));
  }
}

interface PromiseMonad extends PromisePure, Monad<Promise_> {

  static PromiseMonad instance(Executor executor) {
    return () -> executor;
  }

  @Override
  default <T, R> Kind<Promise_, R> flatMap(Kind<Promise_, ? extends T> value,
      Function1<? super T, ? extends Kind<Promise_, ? extends R>> map) {
    return value.fix(toPromise()).flatMap(map.andThen(PromiseOf::narrowK));
  }

  /**
   * XXX In order to create real parallel computations, we need to override ap to use the
   * applicative version of the ap method
   */
  @Override
  default <T, R> Kind<Promise_, R> ap(Kind<Promise_, ? extends T> value, 
      Kind<Promise_, ? extends Function1<? super T, ? extends R>> apply) {
    return PromiseInstances.applicative(executor()).ap(value, apply);
  }
}
