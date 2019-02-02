/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.BiFunctor;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface Either<L, R> extends FlatMap2<Either.µ, L, R>, Holder<R> {

  final class µ implements Kind {}

  static <L, R> Either<L, R> left(L value) {
    return new Left<>(value);
  }

  static <L, R> Either<L, R> right(R value) {
    return new Right<>(value);
  }

  static <L, R> Either<L, R> narrowK(Higher2<Either.µ, L, R> hkt) {
    return (Either<L, R>) hkt;
  }

  static <L, R> Either<L, R> narrowK(Higher1<Higher1<Either.µ, L>, R> hkt) {
    return (Either<L, R>) hkt;
  }

  boolean isLeft();
  boolean isRight();
  L getLeft();
  R getRight();

  @Override
  default R get() {
    if (isRight()) {
      return getRight();
    }
    throw new NoSuchElementException("get() on left");
  }

  default Option<L> left() {
    if (isLeft()) {
      return Option.some(getLeft());
    }
    return Option.none();
  }

  default Option<R> right() {
    if (isRight()) {
      return Option.some(getRight());
    }
    return Option.none();
  }

  default Either<R, L> swap() {
    if (isRight()) {
      return left(getRight());
    }
    return right(getLeft());
  }

  default <T, U> Either<T, U> bimap(Function1<L, T> leftMapper, Function1<R, U> rightMapper) {
    if (isRight()) {
      return right(rightMapper.apply(getRight()));
    }
    return left(leftMapper.apply(getLeft()));
  }

  @Override
  default <T> Either<L, T> map(Function1<R, T> map) {
    return bimap(Function1.identity(), map);
  }

  default <T> Either<T, R> mapLeft(Function1<L, T> map) {
    return bimap(map, Function1.identity());
  }

  @Override
  default <T> Either<L, T> flatMap(Function1<R, ? extends Higher2<Either.µ, L, T>> map) {
    if (isRight()) {
      return map.andThen(Either::narrowK).apply(getRight());
    }
    return left(getLeft());
  }

  default <T> Either<T, R> flatMapLeft(Function1<L, ? extends Higher2<Either.µ, T, R>> map) {
    if (isLeft()) {
      return map.andThen(Either::narrowK).apply(getLeft());
    }
    return right(getRight());
  }

  default Option<Either<L, R>> filter(Matcher1<R> matcher) {
    if (isRight() && matcher.match(getRight())) {
      return Option.some(this);
    }
    return Option.none();
  }

  default Either<L, R> filterOrElse(Matcher1<R> matcher, Producer<Either<L, R>> orElse) {
    if (isLeft() || matcher.match(getRight())) {
      return this;
    }
    return orElse.get();
  }

  default Either<L, R> orElse(Either<L, R> orElse) {
    if (isLeft()) {
      return orElse;
    }
    return this;
  }

  default R getOrElse(R value) {
    return getOrElse(Producer.cons(value));
  }

  default R getOrElse(Producer<R> orElse) {
    return fold(orElse.asFunction(), identity());
  }

  default <T> T fold(Function1<L, T> leftMapper, Function1<R, T> rightMapper) {
    if (isRight()) {
      return rightMapper.apply(getRight());
    }
    return leftMapper.apply(getLeft());
  }

  default Stream<R> stream() {
    return fold(cons(Stream.empty()), Stream::of);
  }

  default Sequence<R> sequence() {
    return fold(cons(ImmutableList.empty()), ImmutableList::of);
  }

  default Option<R> toOption() {
    return fold(cons(Option.none()), Option::some);
  }

  default Validation<L, R> toValidation() {
    return fold(Validation::invalid, Validation::valid);
  }

  @Override
  @SuppressWarnings("unchecked")
  default <V> Either<L, V> flatten() {
    try {
      return ((Either<L, Either<L, V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }

  static <L, R> Eq<Higher2<Either.µ, L, R>> eq(Eq<L> leftEq, Eq<R> rightEq) {
    return (a, b) -> Pattern2.<Either<L, R>, Either<L, R>, Boolean>build()
      .when((x, y) -> x.isLeft() && y.isLeft())
        .then((x, y) -> leftEq.eqv(x.getLeft(), y.getLeft()))
      .when((x, y) -> x.isRight() && y.isRight())
        .then((x, y) -> rightEq.eqv(x.getRight(), y.getRight()))
      .otherwise()
        .returns(false)
      .apply(narrowK(a), narrowK(b));
  }

  static <L> Functor<Higher1<Either.µ, L>> functor() {
    return new EitherFunctor<L>() {};
  }

  static BiFunctor<Either.µ> bifunctor() {
    return new EitherBiFunctor() {};
  }

  static <L> Applicative<Higher1<Either.µ, L>> applicative() {
    return new EitherApplicative<L>() {};
  }

  static <L> Monad<Higher1<Either.µ, L>> monad() {
    return new EitherMonad<L>() {};
  }

  static <L> MonadError<Higher1<Either.µ, L>, L> monadError() {
    return new EitherMonadError<L>() {};
  }

  static <L> Foldable<Higher1<Either.µ, L>> foldable() {
    return new EitherFoldable<L>() {};
  }

  static <L> Traverse<Higher1<Either.µ, L>> traverse() {
    return new EitherTraverse<L>() {};
  }

  EitherModule module();

  final class Left<L, R> implements Either<L, R>, Serializable {

    private static final long serialVersionUID = 7040154642166638129L;

    private L value;

    private Left(L value) {
      this.value = requireNonNull(value);
    }

    @Override
    public boolean isLeft() {
      return true;
    }

    @Override
    public boolean isRight() {
      return false;
    }

    @Override
    public L getLeft() {
      return value;
    }

    @Override
    public R getRight() {
      throw new NoSuchElementException("getRight() in left");
    }

    @Override
    public EitherModule module() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .comparing(Either::getLeft)
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "Left(" + value + ")";
    }
  }

  final class Right<L, R> implements Either<L, R> {

    private R value;

    private Right(R value) {
      this.value = requireNonNull(value);
    }

    @Override
    public boolean isLeft() {
      return false;
    }

    @Override
    public boolean isRight() {
      return true;
    }

    @Override
    public L getLeft() {
      throw new NoSuchElementException("getLeft() in right");
    }

    @Override
    public R getRight() {
      return value;
    }

    @Override
    public EitherModule module() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .comparing(Either::getRight)
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "Right(" + value + ")";
    }
  }
}

interface EitherModule { }

interface EitherFunctor<L> extends Functor<Higher1<Either.µ, L>> {

  @Override
  default <T, R> Either<L, R> map(Higher1<Higher1<Either.µ, L>, T> value, Function1<T, R> map) {
    return Either.narrowK(value).map(map);
  }
}

interface EitherBiFunctor extends BiFunctor<Either.µ> {

  @Override
  default <A, B, C, D> Either<C, D> bimap(Higher2<Either.µ, A, B> value,
      Function1<A, C> leftMap, Function1<B, D> rightMap) {
    return Either.narrowK(value).mapLeft(leftMap).map(rightMap);
  }
}

interface EitherPure<L> extends Applicative<Higher1<Either.µ, L>> {

  @Override
  default <T> Either<L, T> pure(T value) {
    return Either.right(value);
  }
}

interface EitherApply<L> extends Applicative<Higher1<Either.µ, L>> {

  @Override
  default <T, R> Either<L, R> ap(Higher1<Higher1<Either.µ, L>, T> value,
      Higher1<Higher1<Either.µ, L>, Function1<T, R>> apply) {
    return Either.narrowK(value).flatMap(t -> Either.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface EitherApplicative<L> extends EitherPure<L>, EitherApply<L> { }

interface EitherMonad<L> extends EitherPure<L>, Monad<Higher1<Either.µ, L>> {

  @Override
  default <T, R> Either<L, R> flatMap(Higher1<Higher1<Either.µ, L>, T> value,
      Function1<T, ? extends Higher1<Higher1<Either.µ, L>, R>> map) {
    return Either.narrowK(value).flatMap(map.andThen(Either::narrowK));
  }
}

interface EitherMonadError<L> extends EitherMonad<L>, MonadError<Higher1<Either.µ, L>, L> {

  @Override
  default <A> Either<L, A> raiseError(L error) {
    return Either.left(error);
  }

  @Override
  default <A> Either<L, A> handleErrorWith(Higher1<Higher1<Either.µ, L>, A> value,
      Function1<L, ? extends Higher1<Higher1<Either.µ, L>, A>> handler) {
    return Either.narrowK(value).fold(handler.andThen(Either::narrowK), Either::right);
  }
}

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

interface EitherTraverse<L> extends Traverse<Higher1<Either.µ, L>>, EitherFoldable<L> {

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Higher1<Either.µ, L>, R>> traverse(
      Applicative<G> applicative, Higher1<Higher1<Either.µ, L>, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return Either.narrowK(value).fold(
        l -> applicative.pure(Either.left(l)),
        t -> applicative.map(mapper.apply(t), Either::right));
  }
}