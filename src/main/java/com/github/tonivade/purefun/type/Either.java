/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.handler.EitherHandler.identity;
import static com.github.tonivade.purefun.type.Equal.comparing;
import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Functor;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Matcher;
import com.github.tonivade.purefun.Producer;

public interface Either<L, R> extends Functor<R>, Holder<R> {
  
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
    if (isRight()) {
      return right(map.apply(getRight()));
    }
    return left(getLeft());
  }
  
  default <T> Either<T, R> mapLeft(Function1<L, T> map) {
    if (isLeft()) {
      return left(map.apply(getLeft()));
    }
    return right(getRight());
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

  default Option<Either<L, R>> filter(Matcher<R> matcher) {
    if (isRight() && matcher.match(getRight())) {
      return Option.some(this);
    }
    return Option.none();
  }

  default Either<L, R> filterOrElse(Matcher<R> matcher, Producer<Either<L, R>> orElse) {
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
  
  @SuppressWarnings("unchecked")
  default <V> Either<L, V> flatten() {
    try {
      return ((Either<L, Either<L, V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }
  
  final class Left<L, R> implements Either<L, R> {

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
