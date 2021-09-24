/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Function1.cons;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
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
    return (a, b) -> {
      if (a instanceof Try.Failure<T> failureA && b instanceof Try.Failure<T> failureB) {
        return eqFailure.eqv(failureA.getCause(), failureB.getCause());
      }
      if (a instanceof Try.Success<T> successA && b instanceof Try.Success<T> successB) {
        return eqSuccess.eqv(successA.getOrElseThrow(), successB.getOrElseThrow());
      }
      return false;
    };
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
  default <T, R> Kind<Try_, R> map(Kind<Try_, ? extends T> value, Function1<? super T, ? extends R> mapper) {
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
  default <T, R> Kind<Try_, R> ap(Kind<Try_, ? extends T> value, 
      Kind<Try_, ? extends Function1<? super T, ? extends R>> apply) {
    return TryOf.narrowK(value).flatMap(t -> TryOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface TryMonad extends TryPure, Monad<Try_> {

  TryMonad INSTANCE = new TryMonad() {};

  @Override
  default <T, R> Kind<Try_, R> flatMap(Kind<Try_, ? extends T> value,
      Function1<? super T, ? extends Kind<Try_, ? extends R>> map) {
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
      Function1<? super Throwable, ? extends Kind<Try_, ? extends A>> handler) {
    return TryOf.narrowK(value).fold(handler.andThen(TryOf::narrowK), Try::success);
  }
}

interface TryMonadThrow extends TryMonadError, MonadThrow<Try_> {

  TryMonadThrow INSTANCE = new TryMonadThrow() {};
}

interface TryFoldable extends Foldable<Try_> {

  TryFoldable INSTANCE = new TryFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Try_, ? extends A> value, B initial, Function2<? super B, ? super A, ? extends B> mapper) {
    return TryOf.narrowK(value).fold(t -> initial, a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Try_, ? extends A> value, Eval<? extends B> initial,
      Function2<? super A, ? super Eval<? extends B>, ? extends Eval<? extends B>> mapper) {
    return TryOf.<A>narrowK(value).fold(
        cons(initial).andThen(EvalOf::<B>narrowK), 
        a -> mapper.andThen(EvalOf::<B>narrowK).apply(a, initial));
  }
}

interface TryTraverse extends Traverse<Try_>, TryFoldable {

  TryTraverse INSTANCE = new TryTraverse() {};

  @Override
  default <G extends Witness, T, R> Kind<G, Kind<Try_, R>> traverse(
      Applicative<G> applicative, Kind<Try_, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper) {
    return TryOf.narrowK(value).fold(
        t -> applicative.pure(Try.<R>failure(t).kind()),
        t -> {
          Kind<G, ? extends R> apply = mapper.apply(t);
          return applicative.map(apply, x -> Try.success(x));
        });
  }
}
