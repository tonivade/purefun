/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;

public interface EvalInstances {

  static Functor<Eval<?>> functor() {
    return EvalFunctor.INSTANCE;
  }

  static Applicative<Eval<?>> applicative() {
    return EvalApplicative.INSTANCE;
  }

  static Monad<Eval<?>> monad() {
    return EvalMonad.INSTANCE;
  }

  static MonadError<Eval<?>, Throwable> monadError() {
    return EvalMonadError.INSTANCE;
  }

  static MonadThrow<Eval<?>> monadThrow() {
    return EvalMonadThrow.INSTANCE;
  }

  static Comonad<Eval<?>> comonad() {
    return EvalComonad.INSTANCE;
  }

  static Defer<Eval<?>> defer() {
    return EvalDefer.INSTANCE;
  }
}

interface EvalFunctor extends Functor<Eval<?>> {

  EvalFunctor INSTANCE = new EvalFunctor() {};

  @Override
  default <T, R> Eval<R> map(Kind<Eval<?>, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return EvalOf.toEval(value).map(mapper);
  }
}

interface EvalPure extends Applicative<Eval<?>> {

  @Override
  default <T> Kind<Eval<?>, T> pure(T value) {
    return Eval.now(value);
  }
}

interface EvalApplicative extends EvalPure {

  EvalApplicative INSTANCE = new EvalApplicative() {};

  @Override
  default <T, R> Kind<Eval<?>, R> ap(Kind<Eval<?>, ? extends T> value,
      Kind<Eval<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return EvalOf.toEval(value).flatMap(t -> EvalOf.toEval(apply).map(f -> f.apply(t)));
  }
}

interface EvalMonad extends EvalPure, Monad<Eval<?>> {

  EvalMonad INSTANCE = new EvalMonad() {};

  @Override
  default <T, R> Kind<Eval<?>, R> flatMap(Kind<Eval<?>, ? extends T> value, Function1<? super T, ? extends Kind<Eval<?>, ? extends R>> map) {
    return EvalOf.toEval(value).flatMap(map.andThen(EvalOf::<R>toEval));
  }
}

interface EvalMonadError extends EvalMonad, MonadError<Eval<?>, Throwable> {

  EvalMonadError INSTANCE = new EvalMonadError() {};

  @Override
  default <A> Kind<Eval<?>, A> raiseError(Throwable error) {
    return Eval.raiseError(error);
  }

  @Override
  default <A> Kind<Eval<?>, A> handleErrorWith(
      Kind<Eval<?>, A> value, Function1<? super Throwable, ? extends Kind<Eval<?>, ? extends A>> handler) {
    Eval<Try<A>> attempt = Eval.always(() -> Try.of(value.<Eval<A>>fix()::value));
    return attempt.flatMap(try_ -> try_.fold(handler.andThen(EvalOf::toEval), Eval::now));
  }
}

interface EvalMonadThrow extends EvalMonadError, MonadThrow<Eval<?>> {

  EvalMonadThrow INSTANCE = new EvalMonadThrow() {};
}

interface EvalComonad extends EvalFunctor, Comonad<Eval<?>> {

  EvalComonad INSTANCE = new EvalComonad() {};

  @Override
  default <A, B> Kind<Eval<?>, B> coflatMap(Kind<Eval<?>, ? extends A> value, Function1<? super Kind<Eval<?>, ? extends A>, ? extends B> map) {
    return Eval.later(() -> map.apply(value));
  }

  @Override
  default <A> A extract(Kind<Eval<?>, ? extends A> value) {
    return EvalOf.toEval(value).value();
  }
}

interface EvalDefer extends Defer<Eval<?>> {

  EvalDefer INSTANCE = new EvalDefer() {};

  @Override
  default <A> Kind<Eval<?>, A> defer(Producer<? extends Kind<Eval<?>, ? extends A>> defer) {
    return Eval.defer(defer.map(EvalOf::toEval));
  }
}