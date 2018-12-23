/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.handler.EitherHandler.identity;
import static com.github.tonivade.purefun.typeclasses.Eq.comparing;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
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
import com.github.tonivade.purefun.typeclasses.Equal;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;

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

  default R orElse(R value) {
    return orElse(Producer.unit(value));
  }

  default R orElse(Producer<R> orElse) {
    if (isRight()) {
      return getRight();
    }
    return orElse.get();
  }

  default <T> T fold(Function1<L, T> leftMapper, Function1<R, T> rightMapper) {
    if (isRight()) {
      return rightMapper.apply(getRight());
    }
    return leftMapper.apply(getLeft());
  }

  default Stream<R> stream() {
    if (isRight()) {
      return Stream.of(getRight());
    }
    return Stream.empty();
  }

  default Sequence<R> sequence() {
    if (isRight()) {
      return ImmutableList.of(get());
    }
    return ImmutableList.empty();
  }

  default Option<R> toOption() {
    if (isRight()) {
      return Option.some(getRight());
    }
    return Option.none();
  }

  default Validation<L, R> toValidation() {
    if (isRight()) {
      return Validation.valid(getRight());
    }
    return Validation.invalid(getLeft());
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
    return new Functor<Higher1<Either.µ, L>>() {

      @Override
      public <T, R> Either<L, R> map(Higher1<Higher1<Either.µ, L>, T> value, Function1<T, R> map) {
        return narrowK(value).map(map);
      }
    };
  }

  static BiFunctor<Either.µ> bifunctor() {
    return new BiFunctor<Either.µ>() {

      @Override
      public <A, B, C, D> Either<C, D> bimap(Higher2<Either.µ, A, B> value, Function1<A, C> leftMap, Function1<B, D> rightMap) {
        return narrowK(value).bimap(leftMap, rightMap);
      }
    };
  }

  static <L> Applicative<Higher1<Either.µ, L>> applicative() {
    return new Applicative<Higher1<Either.µ, L>>() {

      @Override
      public <T> Either<L, T> pure(T value) {
        return right(value);
      }

      @Override
      public <T, R> Either<L, R> ap(Higher1<Higher1<Either.µ, L>, T> value,
          Higher1<Higher1<Either.µ, L>, Function1<T, R>> apply) {
        return narrowK(value).flatMap(t -> narrowK(apply).map(f -> f.apply(t)));
      }
    };
  }

  static <L> Monad<Higher1<Either.µ, L>> monad() {
    return new Monad<Higher1<Either.µ, L>>() {

      @Override
      public <T> Either<L, T> pure(T value) {
        return right(value);
      }

      @Override
      public <T, R> Either<L, R> flatMap(Higher1<Higher1<Either.µ, L>, T> value,
                                         Function1<T, ? extends Higher1<Higher1<Either.µ, L>, R>> map) {
        return narrowK(value).flatMap(map.andThen(Either::narrowK));
      }
    };
  }

  static <E> MonadError<Higher1<Either.µ, E>, E> monadError() {
    return new MonadError<Higher1<Either.µ, E>, E>() {

      @Override
      public <T> Either<E, T> pure(T value) {
        return right(value);
      }

      @Override
      public <A> Either<E, A> raiseError(E error) {
        return left(error);
      }

      @Override
      public <T, R> Either<E, R> flatMap(Higher1<Higher1<Either.µ, E>, T> value,
                                         Function1<T, ? extends Higher1<Higher1<Either.µ, E>, R>> map) {
        return narrowK(value).flatMap(map.andThen(Either::narrowK));
      }

      @Override
      public <A> Either<E, A> handleErrorWith(Higher1<Higher1<Either.µ, E>, A> value,
                                              Function1<E, ? extends Higher1<Higher1<Either.µ, E>, A>> handler) {
        return narrowK(value).fold(handler.andThen(Either::narrowK), Either::right);
      }
    };
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
          .append(comparing(Either::getLeft))
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
          .append(comparing(Either::getRight))
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "Right(" + value + ")";
    }
  }
}

interface EitherModule {

}