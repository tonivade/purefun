/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
import com.github.tonivade.purefun.type.Eval_;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;

public interface EvalInstances {

  static Functor<Eval_> functor() {
    return EvalFunctor.INSTANCE;
  }

  static Applicative<Eval_> applicative() {
    return EvalApplicative.INSTANCE;
  }

  static Monad<Eval_> monad() {
    return EvalMonad.INSTANCE;
  }

  static MonadError<Eval_, Throwable> monadError() {
    return EvalMonadError.INSTANCE;
  }

  static MonadThrow<Eval_> monadThrow() {
    return EvalMonadThrow.INSTANCE;
  }

  static Comonad<Eval_> comonad() {
    return EvalComonad.INSTANCE;
  }

  static Defer<Eval_> defer() {
    return EvalDefer.INSTANCE;
  }
}

interface EvalFunctor extends Functor<Eval_> {

  EvalFunctor INSTANCE = new EvalFunctor() {};

  @Override
  default <T, R> Kind<Eval_, R> map(Kind<Eval_, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return EvalOf.narrowK(value).map(mapper);
  }
}

interface EvalPure extends Applicative<Eval_> {

  @Override
  default <T> Kind<Eval_, T> pure(T value) {
    return Eval.now(value);
  }
}

interface EvalApplicative extends EvalPure {

  EvalApplicative INSTANCE = new EvalApplicative() {};

  @Override
  default <T, R> Kind<Eval_, R> ap(Kind<Eval_, ? extends T> value, Kind<Eval_, Function1<? super T, ? extends R>> apply) {
    return EvalOf.narrowK(value).flatMap(t -> EvalOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface EvalMonad extends EvalPure, Monad<Eval_> {

  EvalMonad INSTANCE = new EvalMonad() {};

  @Override
  default <T, R> Kind<Eval_, R> flatMap(Kind<Eval_, ? extends T> value, Function1<? super T, ? extends Kind<Eval_, ? extends R>> map) {
    return EvalOf.narrowK(value).flatMap(map.andThen(EvalOf::<R>narrowK));
  }
}

interface EvalMonadError extends EvalMonad, MonadError<Eval_, Throwable> {

  EvalMonadError INSTANCE = new EvalMonadError() {};

  @Override
  default <A> Kind<Eval_, A> raiseError(Throwable error) {
    return Eval.<A>raiseError(error);
  }

  @Override
  default <A> Kind<Eval_, A> handleErrorWith(
      Kind<Eval_, A> value, Function1<? super Throwable, ? extends Kind<Eval_, ? extends A>> handler) {
    Eval<Try<A>> attempt = Eval.always(() -> Try.of(value.fix(EvalOf::narrowK)::value));
    return attempt.flatMap(try_ -> try_.fold(handler.andThen(EvalOf::narrowK), Eval::now));
  }
}

interface EvalMonadThrow extends EvalMonadError, MonadThrow<Eval_> {

  EvalMonadThrow INSTANCE = new EvalMonadThrow() {};
}

interface EvalComonad extends EvalFunctor, Comonad<Eval_> {

  EvalComonad INSTANCE = new EvalComonad() {};

  @Override
  default <A, B> Kind<Eval_, B> coflatMap(Kind<Eval_, ? extends A> value, Function1<? super Kind<Eval_, ? extends A>, ? extends B> map) {
    return Eval.later(() -> map.apply(value));
  }

  @Override
  default <A> A extract(Kind<Eval_, ? extends A> value) {
    return EvalOf.narrowK(value).value();
  }
}

interface EvalDefer extends Defer<Eval_> {

  EvalDefer INSTANCE = new EvalDefer() {};

  @Override
  default <A> Kind<Eval_, A> defer(Producer<? extends Kind<Eval_, ? extends A>> defer) {
    return Eval.defer(defer.map(EvalOf::narrowK));
  }
}