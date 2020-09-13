/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;
import com.github.tonivade.purefun.type.Try_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface TryInstances {

  static <T> Eq<Kind<Try_, T>> eq(Eq<T> eqSuccess) {
    final Eq<Throwable> eqFailure = Eq.throwable();
    return (a, b) -> Pattern2.<Try<T>, Try<T>, Boolean>build()
      .when((x, y) -> x.isFailure() && y.isFailure())
        .then((x, y) -> eqFailure.eqv(x.getCause(), y.getCause()))
      .when((x, y) -> x.isSuccess() && y.isSuccess())
        .then((x, y) -> eqSuccess.eqv(x.get(), y.get()))
      .otherwise()
        .returns(false)
      .apply(TryOf.narrowK(a), TryOf.narrowK(b));
  }

  static Functor<Try_> functor() {
    return TryFunctor.INSTANCE;
  }

  static Applicative<Try_> applicative() {
    return TryApplicative.INSTANCE;
  }

  static Monad<Try_> monad() {
    return TryMonad.INSTANCE;
  }

  static MonadError<Try_, Throwable> monadError() {
    return TryMonadError.INSTANCE;
  }

  static MonadThrow<Try_> monadThrow() {
    return TryMonadThrow.INSTANCE;
  }

  static Foldable<Try_> foldable() {
    return TryFoldable.INSTANCE;
  }

  static Traverse<Try_> traverse() {
    return TryTraverse.INSTANCE;
  }
}

interface TryFunctor extends Functor<Try_> {

  TryFunctor INSTANCE = new TryFunctor() {};

  @Override
  default <T, R> Kind<Try_, R> map(Kind<Try_, T> value, Function1<T, R> mapper) {
    return TryOf.narrowK(value).map(mapper);
  }
}

interface TryPure extends Applicative<Try_> {

  @Override
  default <T> Kind<Try_, T> pure(T value) {
    return Try.success(value);
  }
}

interface TryApplicative extends TryPure {

  TryApplicative INSTANCE = new TryApplicative() {};

  @Override
  default <T, R> Kind<Try_, R> ap(Kind<Try_, T> value, Kind<Try_, Function1<T, R>> apply) {
    return TryOf.narrowK(value).flatMap(t -> TryOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface TryMonad extends TryPure, Monad<Try_> {

  TryMonad INSTANCE = new TryMonad() {};

  @Override
  default <T, R> Kind<Try_, R> flatMap(Kind<Try_, T> value,
      Function1<T, ? extends Kind<Try_, R>> map) {
    return TryOf.narrowK(value).flatMap(map.andThen(TryOf::narrowK));
  }
}

interface TryMonadError extends TryMonad, MonadError<Try_, Throwable> {

  TryMonadError INSTANCE = new TryMonadError() {};

  @Override
  default <A> Kind<Try_, A> raiseError(Throwable error) {
    return Try.<A>failure(error);
  }

  @Override
  default <A> Kind<Try_, A> handleErrorWith(Kind<Try_, A> value,
      Function1<Throwable, ? extends Kind<Try_, A>> handler) {
    return TryOf.narrowK(value).fold(handler.andThen(TryOf::narrowK), Try::success);
  }
}

interface TryMonadThrow extends TryMonadError, MonadThrow<Try_> {

  TryMonadThrow INSTANCE = new TryMonadThrow() {};
}

interface TryFoldable extends Foldable<Try_> {

  TryFoldable INSTANCE = new TryFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Try_, A> value, B initial, Function2<B, A, B> mapper) {
    return TryOf.narrowK(value).fold(t -> initial, a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Try_, A> value, Eval<B> initial,
      Function2<A, Eval<B>, Eval<B>> mapper) {
    return TryOf.narrowK(value).fold(t -> initial, a -> mapper.apply(a, initial));
  }
}

interface TryTraverse extends Traverse<Try_>, TryFoldable {

  TryTraverse INSTANCE = new TryTraverse() {};

  @Override
  default <G extends Witness, T, R> Kind<G, Kind<Try_, R>> traverse(
      Applicative<G> applicative, Kind<Try_, T> value,
      Function1<T, Kind<G, ? extends R>> mapper) {
    return TryOf.narrowK(value).fold(
        t -> applicative.pure(Try.<R>failure(t)),
        t -> applicative.map(mapper.apply(t), x -> Try.success(x)));
  }
}
