/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Eval;
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
    return EvalFunctor.instance();
  }

  static Applicative<Eval_> applicative() {
    return EvalApplicative.instance();
  }

  static Monad<Eval_> monad() {
    return EvalMonad.instance();
  }

  static MonadError<Eval_, Throwable> monadError() {
    return EvalMonadError.instance();
  }

  static MonadThrow<Eval_> monadThrow() {
    return EvalMonadThrow.instance();
  }

  static Comonad<Eval_> comonad() {
    return EvalComonad.instance();
  }

  static Defer<Eval_> defer() {
    return EvalDefer.instance();
  }
}

@Instance
interface EvalFunctor extends Functor<Eval_> {

  @Override
  default <T, R> Higher1<Eval_, R> map(Higher1<Eval_, T> value, Function1<T, R> mapper) {
    return Eval_.narrowK(value).map(mapper);
  }
}

interface EvalPure extends Applicative<Eval_> {

  @Override
  default <T> Higher1<Eval_, T> pure(T value) {
    return Eval.now(value);
  }
}

@Instance
interface EvalApplicative extends EvalPure {

  @Override
  default <T, R> Higher1<Eval_, R> ap(Higher1<Eval_, T> value, Higher1<Eval_, Function1<T, R>> apply) {
    return Eval_.narrowK(value).flatMap(t -> Eval_.narrowK(apply).map(f -> f.apply(t)));
  }
}

@Instance
interface EvalMonad extends EvalPure, Monad<Eval_> {

  @Override
  default <T, R> Higher1<Eval_, R> flatMap(Higher1<Eval_, T> value, Function1<T, ? extends Higher1<Eval_, R>> map) {
    return Eval_.narrowK(value).flatMap(map.andThen(Eval_::<R>narrowK));
  }
}

@Instance
interface EvalMonadError extends EvalMonad, MonadError<Eval_, Throwable> {

  @Override
  default <A> Higher1<Eval_, A> raiseError(Throwable error) {
    return Eval.<A>raiseError(error);
  }

  @Override
  default <A> Higher1<Eval_, A> handleErrorWith(
      Higher1<Eval_, A> value, Function1<Throwable, ? extends Higher1<Eval_, A>> handler) {
    Eval<Try<A>> attempt = Eval.always(() -> Try.of(value.fix1(Eval_::narrowK)::value));
    return attempt.flatMap(try_ -> try_.fold(handler.andThen(Eval_::narrowK), Eval::now));
  }
}

@Instance
interface EvalMonadThrow extends EvalMonadError, MonadThrow<Eval_> { }

@Instance
interface EvalComonad extends EvalFunctor, Comonad<Eval_> {

  @Override
  default <A, B> Higher1<Eval_, B> coflatMap(Higher1<Eval_, A> value, Function1<Higher1<Eval_, A>, B> map) {
    return Eval.later(() -> map.apply(value));
  }

  @Override
  default <A> A extract(Higher1<Eval_, A> value) {
    return Eval_.narrowK(value).value();
  }
}

@Instance
interface EvalDefer extends Defer<Eval_> {

  @Override
  default <A> Higher1<Eval_, A> defer(Producer<Higher1<Eval_, A>> defer) {
    return Eval.defer(defer.map(Eval_::narrowK));
  }
}