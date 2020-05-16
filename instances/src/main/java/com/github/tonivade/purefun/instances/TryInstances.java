/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.type.Try_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface TryInstances {

  static <T> Eq<Higher1<Try_, T>> eq(Eq<T> eqSuccess) {
    final Eq<Throwable> eqFailure = Eq.throwable();
    return (a, b) -> Pattern2.<Try<T>, Try<T>, Boolean>build()
      .when((x, y) -> x.isFailure() && y.isFailure())
        .then((x, y) -> eqFailure.eqv(x.getCause(), y.getCause()))
      .when((x, y) -> x.isSuccess() && y.isSuccess())
        .then((x, y) -> eqSuccess.eqv(x.get(), y.get()))
      .otherwise()
        .returns(false)
      .apply(Try_.narrowK(a), Try_.narrowK(b));
  }

  static Functor<Try_> functor() {
    return TryFunctor.instance();
  }

  static Applicative<Try_> applicative() {
    return TryApplicative.instance();
  }

  static Monad<Try_> monad() {
    return TryMonad.instance();
  }

  static MonadError<Try_, Throwable> monadError() {
    return TryMonadError.instance();
  }

  static MonadThrow<Try_> monadThrow() {
    return TryMonadThrow.instance();
  }

  static Foldable<Try_> foldable() {
    return TryFoldable.instance();
  }

  static Traverse<Try_> traverse() {
    return TryTraverse.instance();
  }
}

@Instance
interface TryFunctor extends Functor<Try_> {

  @Override
  default <T, R> Higher1<Try_, R> map(Higher1<Try_, T> value, Function1<T, R> mapper) {
    return Try_.narrowK(value).map(mapper).kind1();
  }
}

interface TryPure extends Applicative<Try_> {

  @Override
  default <T> Higher1<Try_, T> pure(T value) {
    return Try.success(value).kind1();
  }
}

@Instance
interface TryApplicative extends TryPure {

  @Override
  default <T, R> Higher1<Try_, R> ap(Higher1<Try_, T> value, Higher1<Try_, Function1<T, R>> apply) {
    return Try_.narrowK(value).flatMap(t -> Try_.narrowK(apply).map(f -> f.apply(t))).kind1();
  }
}

@Instance
interface TryMonad extends TryPure, Monad<Try_> {

  @Override
  default <T, R> Higher1<Try_, R> flatMap(Higher1<Try_, T> value,
      Function1<T, ? extends Higher1<Try_, R>> map) {
    return Try_.narrowK(value).flatMap(map.andThen(Try_::narrowK)).kind1();
  }
}

@Instance
interface TryMonadError extends TryMonad, MonadError<Try_, Throwable> {

  @Override
  default <A> Higher1<Try_, A> raiseError(Throwable error) {
    return Try.<A>failure(error).kind1();
  }

  @Override
  default <A> Higher1<Try_, A> handleErrorWith(Higher1<Try_, A> value,
      Function1<Throwable, ? extends Higher1<Try_, A>> handler) {
    return Try_.narrowK(value).fold(handler.andThen(Try_::narrowK), Try::success).kind1();
  }
}

@Instance
interface TryMonadThrow extends TryMonadError, MonadThrow<Try_> { }

@Instance
interface TryFoldable extends Foldable<Try_> {

  @Override
  default <A, B> B foldLeft(Higher1<Try_, A> value, B initial, Function2<B, A, B> mapper) {
    return Try_.narrowK(value).fold(t -> initial, a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Higher1<Try_, A> value, Eval<B> initial,
      Function2<A, Eval<B>, Eval<B>> mapper) {
    return Try_.narrowK(value).fold(t -> initial, a -> mapper.apply(a, initial));
  }
}

@Instance
interface TryTraverse extends Traverse<Try_>, TryFoldable {

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Try_, R>> traverse(
      Applicative<G> applicative, Higher1<Try_, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return Try_.narrowK(value).fold(
        t -> applicative.pure(Try.<R>failure(t).kind1()),
        t -> applicative.map(mapper.apply(t), x -> Try.success(x).kind1()));
  }
}
