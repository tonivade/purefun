/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Function1.cons;
import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.EitherOf;
import com.github.tonivade.purefun.type.Either_;
import com.github.tonivade.purefun.type.Eval;
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
    return (a, b) -> Pattern2.<Either<L, R>, Either<L, R>, Boolean>build()
      .when((x, y) -> x.isLeft() && y.isLeft())
        .then((x, y) -> leftEq.eqv(x.getLeft(), y.getLeft()))
      .when((x, y) -> x.isRight() && y.isRight())
        .then((x, y) -> rightEq.eqv(x.getRight(), y.getRight()))
      .otherwise()
        .returns(false)
      .apply(EitherOf.narrowK(a), EitherOf.narrowK(b));
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
  default <T, R> Either<L, R> map(Kind<Kind<Either_, L>, T> value, Function1<T, R> map) {
    return EitherOf.narrowK(value).map(map);
  }
}

interface EitherBifunctor extends Bifunctor<Either_> {

  EitherBifunctor INSTANCE = new EitherBifunctor() {};

  @Override
  default <A, B, C, D> Either<C, D> bimap(Kind<Kind<Either_, A>, B> value,
      Function1<A, C> leftMap, Function1<B, D> rightMap) {
    return EitherOf.narrowK(value).mapLeft(leftMap).map(rightMap);
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
  default <T, R> Either<L, R> ap(Kind<Kind<Either_, L>, T> value,
      Kind<Kind<Either_, L>, Function1<T, R>> apply) {
    return EitherOf.narrowK(value).flatMap(t -> EitherOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface EitherMonad<L> extends EitherPure<L>, Monad<Kind<Either_, L>> {

  @SuppressWarnings("rawtypes")
  EitherMonad INSTANCE = new EitherMonad() {};

  @Override
  default <T, R> Either<L, R> flatMap(Kind<Kind<Either_, L>, T> value,
      Function1<T, ? extends Kind<Kind<Either_, L>, R>> map) {
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
      Function1<L, ? extends Kind<Kind<Either_, L>, A>> handler) {
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
  default <A, B> B foldLeft(Kind<Kind<Either_, L>, A> value, B initial, Function2<B, A, B> mapper) {
    return EitherOf.narrowK(value).fold(cons(initial), a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Kind<Either_, L>, A> value, Eval<B> initial,
      Function2<A, Eval<B>, Eval<B>> mapper) {
    return EitherOf.narrowK(value).fold(cons(initial), a -> mapper.apply(a, initial));
  }
}

interface EitherTraverse<L> extends Traverse<Kind<Either_, L>>, EitherFoldable<L> {

  @SuppressWarnings("rawtypes")
  EitherTraverse INSTANCE = new EitherTraverse() {};

  @Override
  default <G extends Witness, T, R> Kind<G, Kind<Kind<Either_, L>, R>> traverse(
      Applicative<G> applicative, Kind<Kind<Either_, L>, T> value,
      Function1<T, Kind<G, ? extends R>> mapper) {
    return EitherOf.narrowK(value).fold(
      l -> applicative.pure(Either.<L, R>left(l)),
      t -> applicative.map(mapper.apply(t), r -> Either.<L, R>right(r)));
  }
}
