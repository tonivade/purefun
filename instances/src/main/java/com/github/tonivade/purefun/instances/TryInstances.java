/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Function1.cons;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Trampoline;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface TryInstances {

  static <T> Eq<Kind<Try<?>, T>> eq(Eq<T> eqSuccess) {
    final Eq<Throwable> eqFailure = Eq.throwable();
    return (a, b) -> {
      if (a instanceof Try.Failure(var causeA) && b instanceof Try.Failure(var causeB)) {
        return eqFailure.eqv(causeA, causeB);
      }
      if (a instanceof Try.Success<T>(var valueA) && b instanceof Try.Success<T>(var valueB)) {
        return eqSuccess.eqv(valueA, valueB);
      }
      return false;
    };
  }

  static Functor<Try<?>> functor() {
    return TryFunctor.INSTANCE;
  }

  static Applicative<Try<?>> applicative() {
    return TryApplicative.INSTANCE;
  }

  static Monad<Try<?>> monad() {
    return TryMonad.INSTANCE;
  }

  static MonadError<Try<?>, Throwable> monadError() {
    return TryMonadError.INSTANCE;
  }

  static MonadThrow<Try<?>> monadThrow() {
    return TryMonadThrow.INSTANCE;
  }

  static Foldable<Try<?>> foldable() {
    return TryFoldable.INSTANCE;
  }

  static Traverse<Try<?>> traverse() {
    return TryTraverse.INSTANCE;
  }
}

interface TryFunctor extends Functor<Try<?>> {

  TryFunctor INSTANCE = new TryFunctor() {};

  @Override
  default <T, R> Kind<Try<?>, R> map(Kind<Try<?>, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return TryOf.toTry(value).map(mapper);
  }
}

interface TryPure extends Applicative<Try<?>> {

  @Override
  default <T> Kind<Try<?>, T> pure(T value) {
    return Try.success(value);
  }
}

interface TryApplicative extends TryPure {

  TryApplicative INSTANCE = new TryApplicative() {};

  @Override
  default <T, R> Kind<Try<?>, R> ap(Kind<Try<?>, ? extends T> value,
      Kind<Try<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return TryOf.toTry(value).flatMap(t -> TryOf.toTry(apply).map(f -> f.apply(t)));
  }
}

interface TryMonad extends TryPure, Monad<Try<?>> {

  TryMonad INSTANCE = new TryMonad() {};

  @Override
  default <T, R> Kind<Try<?>, R> flatMap(Kind<Try<?>, ? extends T> value,
      Function1<? super T, ? extends Kind<Try<?>, ? extends R>> map) {
    return TryOf.toTry(value).flatMap(map.andThen(TryOf::toTry));
  }

  @Override
  default <T, R> Kind<Try<?>, R> tailRecM(T value, Function1<T, ? extends Kind<Try<?>, Either<T, R>>> map) {
    return loop(value, map).run();
  }

  private <T, R> Trampoline<Kind<Try<?>, R>> loop(T value, Function1<T, ? extends Kind<Try<?>, Either<T, R>>> map) {
    return switch (map.andThen(TryOf::toTry).apply(value)) {
      case Try.Failure<Either<T, R>>(var error) -> Trampoline.done(Try.failure(error));
      case Try.Success<Either<T, R>>(Either.Right<T, R>(var right)) -> Trampoline.done(Try.success(right));
      case Try.Success<Either<T, R>>(Either.Left<T, R>(var left)) -> Trampoline.more(() -> loop(left, map));
    };
  }
}

interface TryMonadError extends TryMonad, MonadError<Try<?>, Throwable> {

  TryMonadError INSTANCE = new TryMonadError() {};

  @Override
  default <A> Kind<Try<?>, A> raiseError(Throwable error) {
    return Try.failure(error);
  }

  @Override
  default <A> Kind<Try<?>, A> handleErrorWith(Kind<Try<?>, A> value,
      Function1<? super Throwable, ? extends Kind<Try<?>, ? extends A>> handler) {
    return TryOf.toTry(value).fold(handler.andThen(TryOf::toTry), Try::success);
  }
}

interface TryMonadThrow extends TryMonadError, MonadThrow<Try<?>> {

  TryMonadThrow INSTANCE = new TryMonadThrow() {};
}

interface TryFoldable extends Foldable<Try<?>> {

  TryFoldable INSTANCE = new TryFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Try<?>, ? extends A> value, B initial, Function2<? super B, ? super A, ? extends B> mapper) {
    return TryOf.toTry(value).fold(t -> initial, a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Try<?>, ? extends A> value, Eval<? extends B> initial,
      Function2<? super A, ? super Eval<? extends B>, ? extends Eval<? extends B>> mapper) {
    return TryOf.<A>toTry(value).fold(
        cons(initial).andThen(EvalOf::<B>toEval),
        a -> mapper.andThen(EvalOf::<B>toEval).apply(a, initial));
  }
}

interface TryTraverse extends Traverse<Try<?>>, TryFoldable {

  TryTraverse INSTANCE = new TryTraverse() {};

  @Override
  default <G, T, R> Kind<G, Kind<Try<?>, R>> traverse(
      Applicative<G> applicative, Kind<Try<?>, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper) {
    return TryOf.toTry(value).fold(
        t -> applicative.pure(Try.<R>failure(t).kind()),
        t -> {
          Kind<G, ? extends R> apply = mapper.apply(t);
          return applicative.map(apply, Try::success);
        });
  }
}
