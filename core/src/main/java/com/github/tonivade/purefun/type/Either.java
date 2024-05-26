/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.core.Function1.cons;
import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Kind2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nullable;
import com.github.tonivade.purefun.core.Applicable;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.data.Sequence;

/**
 * <p>This type represents two possible values, called left or right. In general left case is
 * considered the negative case, and right case the positive case, so is right biased</p>
 * <ul>
 *   <li>{@code Either.left(value)}: left value</li>
 *   <li>{@code Either.right(value)}: right value</li>
 * </ul>
 * <p>Usually this type is used to represent a function return type. The left value represents
 * when the function fails, and the right value when function returns normally. It's the functional alternative
 * to exceptions.</p>
 * <p>{@code Try<T>} and {@code Either<Throwable, T>} are isomorphisms.</p>
 * @param <L> type of the left value, negative case
 * @param <R> type of the right value, positive case
 */
@HigherKind
public sealed interface Either<L, R> extends EitherOf<L, R>, Bindable<Either<L, ?>, R>, Applicable<Either<L, ?>, R>, Kind2<Either<?, ?>, L, R> {

  static <L, R> Either<L, R> left(L value) {
    return new Left<>(value);
  }

  static <L, R> Either<L, R> right(R value) {
    return new Right<>(value);
  }

  boolean isLeft();
  boolean isRight();

  /**
   * Returns the left value if available. If not, it throws {@code NoSuchElementException}
   * @return the left value
   * @throws NoSuchElementException if left value is not available
   */
  L getLeft();

  /**
   * Returns the right value if available. If not, it throws {@code NoSuchElementException}
   * @return the right value
   * @throws NoSuchElementException if right value is not available
   */
  R getRight();

  default R get() {
    if (this instanceof Right<L, R>(var right)) {
      return right;
    }
    throw new NoSuchElementException("get() on left");
  }

  default Option<L> left() {
    if (this instanceof Left<L, R>(var left)) {
      return Option.some(left);
    }
    return Option.none();
  }

  default Option<R> right() {
    if (this instanceof Right<L, R>(var right)) {
      return Option.some(right);
    }
    return Option.none();
  }

  default Either<R, L> swap() {
    return switch (this) {
      case Right<L, R>(var right) -> left(right);
      case Left<L, R>(var left) -> right(left);
    };
  }

  default <T, U> Either<T, U> bimap(Function1<? super L, ? extends T> leftMapper, Function1<? super R, ? extends U> rightMapper) {
    return switch (this) {
      case Right<L, R>(var right) -> right(rightMapper.apply(right));
      case Left<L, R>(var left) -> left(leftMapper.apply(left));
    };
  }

  @Override
  default <T> Either<L, T> map(Function1<? super R, ? extends T> map) {
    return bimap(identity(), map);
  }

  @Override
  default <T> Either<L, T> ap(
      Kind<Either<L, ?>, ? extends Function1<? super R, ? extends T>> apply) {
    return apply.fix(EitherOf::toEither).flatMap(this::map);
  }

  default <T> Either<T, R> mapLeft(Function1<? super L, ? extends T> map) {
    return bimap(map, identity());
  }

  @Override
  @SuppressWarnings("unchecked")
  default <T> Either<L, T> flatMap(Function1<? super R, ? extends Kind<Either<L, ?>, ? extends T>> map) {
    if (this instanceof Right<L, R>(var right)) {
      return map.andThen(EitherOf::<L, T>toEither).apply(right);
    }
    return (Either<L, T>) this;
  }

  @SuppressWarnings("unchecked")
  default <T> Either<T, R> flatMapLeft(Function1<? super L, ? extends Either<? extends T, R>> map) {
    if (this instanceof Left<L, R>(var left)) {
      return (Either<T, R>) map.apply(left);
    }
    return (Either<T, R>) this;
  }

  default Option<Either<L, R>> filter(Matcher1<? super R> matcher) {
    if (this instanceof Right<L, R>(var right) && matcher.match(right)) {
      return Option.some(this);
    }
    return Option.none();
  }

  default Option<Either<L, R>> filterNot(Matcher1<? super R> matcher) {
    return filter(matcher.negate());
  }

  default Either<L, R> filterOrElse(Matcher1<? super R> matcher, Producer<? extends Kind<Either<L, ?>, R>> orElse) {
    if (this instanceof Left) {
      return this;
    }
    if (this instanceof Right<L, R>(var right) && matcher.match(right)) {
      return this;
    }
    return orElse.andThen(EitherOf::toEither).get();
  }

  default Either<L, R> or(Producer<Kind<Either<L, ?>, R>> orElse) {
    if (this instanceof Left) {
      return orElse.andThen(EitherOf::toEither).get();
    }
    return this;
  }

  default Either<L, R> orElse(Kind<Either<L, ?>, R> orElse) {
    return or(Producer.cons(orElse));
  }

  default R getOrElse(R value) {
    return getOrElse(Producer.cons(value));
  }

  @Nullable
  default R getOrElseNull() {
    return switch (this) {
      case Left<L, R> l -> null;
      case Right<L, R>(var value) -> value;
    };
  }

  default R getOrElse(Producer<? extends R> orElse) {
    return fold(orElse.asFunction(), identity());
  }

  default <T> T fold(Function1<? super L, ? extends T> leftMapper, Function1<? super R, ? extends T> rightMapper) {
    return switch (this) {
      case Right<L, R>(var right) -> rightMapper.apply(right);
      case Left<L, R>(var left) -> leftMapper.apply(left);
    };
  }

  default Stream<R> stream() {
    return fold(cons(Stream::empty), Stream::of);
  }

  default Sequence<R> sequence() {
    return fold(cons(Sequence::emptyList), Sequence::listOf);
  }

  default Option<R> toOption() {
    return fold(cons(Option::none), Option::some);
  }

  default Validation<L, R> toValidation() {
    return fold(Validation::invalid, Validation::valid);
  }

  static <A> A merge(Either<A, A> either) {
    return either.fold(identity(), identity());
  }

  static <L, A, B, Z> Either<L, Z> map2(
      Either<L, ? extends A> eitherA, Either<L, ? extends B> eitherB,
      Function2<? super A, ? super B, ? extends Z> mapper) {
    return eitherA.flatMap(a -> eitherB.map(b -> mapper.apply(a, b)));
  }

  record Left<L, R>(L value) implements Either<L, R>, Serializable {

    public Left {
      checkNonNull(value);
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
    public String toString() {
      return "Left(" + value + ")";
    }
  }

  record Right<L, R>(R value) implements Either<L, R>, Serializable {

    public Right {
      checkNonNull(value);
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
    public String toString() {
      return "Right(" + value + ")";
    }
  }
}