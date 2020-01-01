/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Function1.cons;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.type.Either;
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

  static <L, R> Eq<Higher2<Either.µ, L, R>> eq(Eq<L> leftEq, Eq<R> rightEq) {
    return (a, b) -> Pattern2.<Either<L, R>, Either<L, R>, Boolean>build()
      .when((x, y) -> x.isLeft() && y.isLeft())
        .then((x, y) -> leftEq.eqv(x.getLeft(), y.getLeft()))
      .when((x, y) -> x.isRight() && y.isRight())
        .then((x, y) -> rightEq.eqv(x.getRight(), y.getRight()))
      .otherwise()
        .returns(false)
      .apply(Either.narrowK(a), Either.narrowK(b));
  }

  static <L> Functor<Higher1<Either.µ, L>> functor() {
    return EitherFunctor.instance();
  }

  static Bifunctor<Either.µ> bifunctor() {
    return EitherBifunctor.instance();
  }

  static <L> Applicative<Higher1<Either.µ, L>> applicative() {
    return EitherApplicative.instance();
  }

  static <L> Monad<Higher1<Either.µ, L>> monad() {
    return EitherMonad.instance();
  }

  static <L> MonadError<Higher1<Either.µ, L>, L> monadError() {
    return EitherMonadError.instance();
  }

  static MonadThrow<Higher1<Either.µ, Throwable>> monadThrow() {
    return EitherMonadThrow.instance();
  }

  static <L> Foldable<Higher1<Either.µ, L>> foldable() {
    return EitherFoldable.instance();
  }

  static <L> Traverse<Higher1<Either.µ, L>> traverse() {
    return EitherTraverse.instance();
  }
}

@Instance
interface EitherFunctor<L> extends Functor<Higher1<Either.µ, L>> {

  @Override
  default <T, R> Higher2<Either.µ, L, R> map(Higher1<Higher1<Either.µ, L>, T> value, Function1<T, R> map) {
    return Either.narrowK(value).map(map).kind2();
  }
}

@Instance
interface EitherBifunctor extends Bifunctor<Either.µ> {

  @Override
  default <A, B, C, D> Higher2<Either.µ, C, D> bimap(Higher2<Either.µ, A, B> value,
      Function1<A, C> leftMap, Function1<B, D> rightMap) {
    return Either.narrowK(value).mapLeft(leftMap).map(rightMap).kind2();
  }
}

interface EitherPure<L> extends Applicative<Higher1<Either.µ, L>> {

  @Override
  default <T> Higher2<Either.µ, L, T> pure(T value) {
    return Either.<L, T>right(value).kind2();
  }
}

@Instance
interface EitherApplicative<L> extends EitherPure<L> {

  @Override
  default <T, R> Higher2<Either.µ, L, R> ap(Higher1<Higher1<Either.µ, L>, T> value,
      Higher1<Higher1<Either.µ, L>, Function1<T, R>> apply) {
    return Either.narrowK(value).flatMap(t -> Either.narrowK(apply).map(f -> f.apply(t))).kind2();
  }
}

@Instance
interface EitherMonad<L> extends EitherPure<L>, Monad<Higher1<Either.µ, L>> {

  @Override
  default <T, R> Higher2<Either.µ, L, R> flatMap(Higher1<Higher1<Either.µ, L>, T> value,
      Function1<T, ? extends Higher1<Higher1<Either.µ, L>, R>> map) {
    return Either.narrowK(value).flatMap(map.andThen(Either::narrowK)).kind2();
  }
}

@Instance
interface EitherMonadError<L> extends EitherMonad<L>, MonadError<Higher1<Either.µ, L>, L> {

  @Override
  default <A> Higher2<Either.µ, L, A> raiseError(L error) {
    return Either.<L, A>left(error).kind2();
  }

  @Override
  default <A> Higher2<Either.µ, L, A> handleErrorWith(Higher1<Higher1<Either.µ, L>, A> value,
      Function1<L, ? extends Higher1<Higher1<Either.µ, L>, A>> handler) {
    return Either.narrowK(value).fold(handler.andThen(Either::narrowK), Either::<L, A>right).kind2();
  }
}

@Instance
interface EitherMonadThrow extends EitherMonadError<Throwable>, MonadThrow<Higher1<Either.µ, Throwable>> { }

@Instance
interface EitherFoldable<L> extends Foldable<Higher1<Either.µ, L>> {

  @Override
  default <A, B> B foldLeft(Higher1<Higher1<Either.µ, L>, A> value, B initial, Function2<B, A, B> mapper) {
    return Either.narrowK(value).fold(cons(initial), a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Higher1<Higher1<Either.µ, L>, A> value, Eval<B> initial,
      Function2<A, Eval<B>, Eval<B>> mapper) {
    return Either.narrowK(value).fold(cons(initial), a -> mapper.apply(a, initial));
  }
}

@Instance
interface EitherTraverse<L> extends Traverse<Higher1<Either.µ, L>>, EitherFoldable<L> {

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Higher1<Either.µ, L>, R>> traverse(
      Applicative<G> applicative, Higher1<Higher1<Either.µ, L>, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return Either.narrowK(value).fold(
      l -> applicative.pure(Either.<L, R>left(l).kind1()),
      t -> applicative.map(mapper.apply(t), r -> Either.<L, R>right(r).kind1()));
  }
}
