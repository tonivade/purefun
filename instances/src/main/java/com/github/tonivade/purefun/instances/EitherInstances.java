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
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.EitherOf;
import com.github.tonivade.purefun.type.Either_;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bifunctor;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Traverse;

@SuppressWarnings("unchecked")
public interface EitherInstances {

  static <L, R> Eq<Kind<Kind<Either_, L>, R>> eq(Eq<L> leftEq, Eq<R> rightEq) {
    return (a, b) -> {
      if (a instanceof Either.Left<L, R> leftA && b instanceof Either.Left<L, R> leftB) {
        return leftEq.eqv(leftA.getLeft(), leftB.getLeft());
      }
      if (a instanceof Either.Right<L, R> rightA && b instanceof Either.Right<L, R> rightB) {
        return rightEq.eqv(rightA.getRight(), rightB.getRight());
      }
      return false;
    };
  }

  static <L> Functor<Kind<Either_, L>> functor() {
    return EitherFunctor.INSTANCE;
  }

  static Bifunctor<Either_> bifunctor() {
    return EitherBifunctor.INSTANCE;
  }

  static <L> Applicative<Kind<Either_, L>> applicative() {
    return EitherApplicative.INSTANCE;
  }

  static <L> Monad<Kind<Either_, L>> monad() {
    return EitherMonad.INSTANCE;
  }

  static <L> MonadError<Kind<Either_, L>, L> monadError() {
    return EitherMonadError.INSTANCE;
  }

  static MonadThrow<Kind<Either_, Throwable>> monadThrow() {
    return EitherMonadThrow.INSTANCE;
  }

  static <L> Foldable<Kind<Either_, L>> foldable() {
    return EitherFoldable.INSTANCE;
  }

  static <L> Traverse<Kind<Either_, L>> traverse() {
    return EitherTraverse.INSTANCE;
  }
}

interface EitherFunctor<L> extends Functor<Kind<Either_, L>> {

  @SuppressWarnings("rawtypes")
  EitherFunctor INSTANCE = new EitherFunctor() {};

  @Override
  default <T, R> Either<L, R> map(Kind<Kind<Either_, L>, ? extends T> value, Function1<? super T, ? extends R> map) {
    return EitherOf.narrowK(value).map(map);
  }
}

interface EitherBifunctor extends Bifunctor<Either_> {

  EitherBifunctor INSTANCE = new EitherBifunctor() {};

  @Override
  default <A, B, C, D> Either<C, D> bimap(Kind<Kind<Either_, A>, ? extends B> value,
      Function1<? super A, ? extends C> leftMap, Function1<? super B, ? extends D> rightMap) {
    return EitherOf.narrowK(value).bimap(leftMap, rightMap);
  }
}

interface EitherPure<L> extends Applicative<Kind<Either_, L>> {

  @Override
  default <T> Either<L, T> pure(T value) {
    return Either.<L, T>right(value);
  }
}

interface EitherApplicative<L> extends EitherPure<L> {

  @SuppressWarnings("rawtypes")
  EitherApplicative INSTANCE = new EitherApplicative() {};

  @Override
  default <T, R> Either<L, R> ap(Kind<Kind<Either_, L>, ? extends T> value,
      Kind<Kind<Either_, L>, ? extends Function1<? super T, ? extends R>> apply) {
    return EitherOf.narrowK(value).flatMap(t -> EitherOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface EitherMonad<L> extends EitherPure<L>, Monad<Kind<Either_, L>> {

  @SuppressWarnings("rawtypes")
  EitherMonad INSTANCE = new EitherMonad() {};

  @Override
  default <T, R> Either<L, R> flatMap(Kind<Kind<Either_, L>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<Either_, L>, ? extends R>> map) {
    return EitherOf.narrowK(value).flatMap(map.andThen(EitherOf::narrowK));
  }
}

interface EitherMonadError<L> extends EitherMonad<L>, MonadError<Kind<Either_, L>, L> {

  @SuppressWarnings("rawtypes")
  EitherMonadError INSTANCE = new EitherMonadError() {};

  @Override
  default <A> Either<L, A> raiseError(L error) {
    return Either.<L, A>left(error);
  }

  @Override
  default <A> Either<L, A> handleErrorWith(Kind<Kind<Either_, L>, A> value,
      Function1<? super L, ? extends Kind<Kind<Either_, L>, ? extends A>> handler) {
    return EitherOf.narrowK(value).fold(handler.andThen(EitherOf::narrowK), Either::<L, A>right);
  }
}

interface EitherMonadThrow extends EitherMonadError<Throwable>, MonadThrow<Kind<Either_, Throwable>> {

  EitherMonadThrow INSTANCE = new EitherMonadThrow() {};
}

interface EitherFoldable<L> extends Foldable<Kind<Either_, L>> {

  @SuppressWarnings("rawtypes")
  EitherFoldable INSTANCE = new EitherFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Kind<Either_, L>, ? extends A> value, B initial, Function2<? super B, ? super A, ? extends B> mapper) {
    return EitherOf.narrowK(value).fold(cons(initial), a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Kind<Either_, L>, ? extends A> value, Eval<? extends B> initial,
      Function2<? super A, ? super Eval<? extends B>, ? extends Eval<? extends B>> mapper) {
    return EitherOf.<L, A>narrowK(value).fold(
        cons(initial).andThen(EvalOf::<B>narrowK), 
        a -> mapper.andThen(EvalOf::<B>narrowK).apply(a, initial));
  }
}

interface EitherTraverse<L> extends Traverse<Kind<Either_, L>>, EitherFoldable<L> {

  @SuppressWarnings("rawtypes")
  EitherTraverse INSTANCE = new EitherTraverse() {};

  @Override
  default <G extends Witness, T, R> Kind<G, Kind<Kind<Either_, L>, R>> traverse(
      Applicative<G> applicative, Kind<Kind<Either_, L>, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper) {
    return EitherOf.narrowK(value).fold(
      l -> applicative.pure(Either.<L, R>left(l).kind()),
      t -> {
        Kind<G, ? extends R> apply = mapper.apply(t);
        return applicative.map(apply, r -> Either.<L, R>right(r));
      });
  }
}
