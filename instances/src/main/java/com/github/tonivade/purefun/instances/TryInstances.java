/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface TryInstances {

  static <T> Eq<Higher1<Try.µ, T>> eq(Eq<T> eqSuccess) {
    final Eq<Throwable> eqFailure = Eq.throwable();
    return (a, b) -> Pattern2.<Try<T>, Try<T>, Boolean>build()
      .when((x, y) -> x.isFailure() && y.isFailure())
        .then((x, y) -> eqFailure.eqv(x.getCause(), y.getCause()))
      .when((x, y) -> x.isSuccess() && y.isSuccess())
        .then((x, y) -> eqSuccess.eqv(x.get(), y.get()))
      .otherwise()
        .returns(false)
      .apply(Try.narrowK(a), Try.narrowK(b));
  }

  static Functor<Try.µ> functor() {
    return new TryFunctor() {};
  }

  static Applicative<Try.µ> applicative() {
    return new TryApplicative() {};
  }

  static Monad<Try.µ> monad() {
    return new TryMonad() {};
  }

  static MonadError<Try.µ, Throwable> monadError() {
    return new TryMonadError() {};
  }

  static MonadThrow<Try.µ> monadThrow() {
    return new TryMonadThrow() {};
  }

  static Foldable<Try.µ> foldable() {
    return new TryFoldable() {};
  }

  static Traverse<Try.µ> traverse() {
    return new TryTraverse() {};
  }
}

@Instance
interface TryFunctor extends Functor<Try.µ> {

  @Override
  default <T, R> Try<R> map(Higher1<Try.µ, T> value, Function1<T, R> mapper) {
    return Try.narrowK(value).map(mapper);
  }
}

@Instance
interface TryPure extends Applicative<Try.µ> {

  @Override
  default <T> Try<T> pure(T value) {
    return Try.success(value);
  }
}

@Instance
interface TryApplicative extends TryPure {

  @Override
  default <T, R> Try<R> ap(Higher1<Try.µ, T> value, Higher1<Try.µ, Function1<T, R>> apply) {
    return Try.narrowK(value).flatMap(t -> Try.narrowK(apply).map(f -> f.apply(t)));
  }
}

@Instance
interface TryMonad extends TryPure, Monad<Try.µ> {

  @Override
  default <T, R> Try<R> flatMap(Higher1<Try.µ, T> value,
      Function1<T, ? extends Higher1<Try.µ, R>> map) {
    return Try.narrowK(value).flatMap(map.andThen(Try::narrowK));
  }
}

@Instance
interface TryMonadError extends TryMonad, MonadError<Try.µ, Throwable> {

  @Override
  default <A> Try<A> raiseError(Throwable error) {
    return Try.failure(error);
  }

  @Override
  default <A> Try<A> handleErrorWith(Higher1<Try.µ, A> value,
      Function1<Throwable, ? extends Higher1<Try.µ, A>> handler) {
    return Try.narrowK(value).fold(handler.andThen(Try::narrowK), Try::success);
  }
}

@Instance
interface TryMonadThrow extends TryMonadError, MonadThrow<Try.µ> { }

@Instance
interface TryFoldable extends Foldable<Try.µ> {

  @Override
  default <A, B> B foldLeft(Higher1<Try.µ, A> value, B initial, Function2<B, A, B> mapper) {
    return Try.narrowK(value).fold(t -> initial, a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Higher1<Try.µ, A> value, Eval<B> initial,
      Function2<A, Eval<B>, Eval<B>> mapper) {
    return Try.narrowK(value).fold(t -> initial, a -> mapper.apply(a, initial));
  }
}

@Instance
interface TryTraverse extends Traverse<Try.µ>, TryFoldable {

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Try.µ, R>> traverse(
      Applicative<G> applicative, Higher1<Try.µ, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return Try.narrowK(value).fold(
        t -> applicative.pure(Try.failure(t)),
        t -> applicative.map(mapper.apply(t), Try::success));
  }
}
