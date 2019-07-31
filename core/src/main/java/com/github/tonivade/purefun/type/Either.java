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
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;

@HigherKind
public interface Either<L, R> {

  static <L, R> Either<L, R> left(L value) {
    return new Left<>(value);
  }

  static <L, R> Either<L, R> right(R value) {
    return new Right<>(value);
  }

  boolean isLeft();
  boolean isRight();
  L getLeft();
  R getRight();

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

  default <T> Either<L, T> map(Function1<R, T> map) {
    return bimap(Function1.identity(), map);
  }

  default <T> Either<T, R> mapLeft(Function1<L, T> map) {
    return bimap(map, Function1.identity());
  }

  default <T> Either<L, T> flatMap(Function1<R, Either<L, T>> map) {
    if (isRight()) {
      return map.apply(getRight());
    }
    return left(getLeft());
  }

  default <T> Either<T, R> flatMapLeft(Function1<L, Either<T, R>> map) {
    if (isLeft()) {
      return map.apply(getLeft());
    }
    return right(getRight());
  }

  default Option<Either<L, R>> filter(Matcher1<R> matcher) {
    if (isRight() && matcher.match(getRight())) {
      return Option.some(this);
    }
    return Option.none();
  }

  default Option<Either<L, R>> filterNot(Matcher1<R> matcher) {
    return filter(matcher.negate());
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