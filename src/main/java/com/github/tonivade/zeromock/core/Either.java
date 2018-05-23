/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static com.github.tonivade.zeromock.core.Equal.equal;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class Either<L, R> {

  private Either() { }
  
  public static <L, R> Either<L, R> left(L value) {
    return new Left<L, R>(value);
  }
  
  public static <L, R> Either<L, R> right(R value) {
    return new Right<L, R>(value);
  }
  
  public abstract boolean isLeft();
  public abstract boolean isRight();
  public abstract L getLeft();
  public abstract R getRight();

  public R get() {
    if (isRight()) {
      return getRight();
    }
    throw new NoSuchElementException("get() on left");
  }
  
  public Option<L> left() {
    if (isLeft()) {
      return Option.some(getLeft());
    }
    return Option.none();
  }
  
  public Option<R> right() {
    if (isRight()) {
      return Option.some(getRight());
    }
    return Option.none();
  }
  
  public Either<R, L> swap() {
    if (isRight()) {
      return left(getRight());
    }
    return right(getLeft());
  }
  
  public <T, U> Either<T, U> bimap(Handler1<L, T> leftMapper, Handler1<R, U> rightMapper) {
    if (isRight()) {
      return right(rightMapper.handle(getRight()));
    }
    return left(leftMapper.handle(getLeft()));
  }
  
  @SuppressWarnings("unchecked")
  public <T> Either<L, T> map(Handler1<R, T> map) {
    if (isRight()) {
      return right(map.handle(getRight()));
    }
    return (Either<L, T>) this;
  }
  
  @SuppressWarnings("unchecked")
  public <T> Either<T, R> mapLeft(Handler1<L, T> map) {
    if (isLeft()) {
      return left(map.handle(getLeft()));
    }
    return (Either<T, R>) this;
  }

  @SuppressWarnings("unchecked")
  public <T> Either<L, T> flatMap(Handler1<R, Either<L, T>> map) {
    if (isRight()) {
      return map.handle(getRight());
    }
    return (Either<L, T>) this;
  }

  @SuppressWarnings("unchecked")
  public <T> Either<T, R> flatMapLeft(Handler1<L, Either<T, R>> map) {
    if (isLeft()) {
      return map.handle(getLeft());
    }
    return (Either<T, R>) this;
  }

  public Option<Either<L, R>> filter(Matcher<R> matcher) {
    if (isRight() && matcher.match(getRight())) {
      return Option.some(this);
    }
    return Option.none();
  }

  public Either<L, R> filterOrElse(Matcher<R> matcher, Handler0<Either<L, R>> orElse) {
    if (isLeft() || matcher.match(getRight())) {
      return this;
    }
    return orElse.handle();
  }

  public R orElse(Handler0<R> orElse) {
    if (isRight()) {
      return getRight();
    }
    return orElse.handle();
  }
  
  public <T> T fold(Handler1<L, T> leftMapper, Handler1<R, T> rightMapper) {
    if (isRight()) {
      return rightMapper.handle(getRight());
    }
    return leftMapper.handle(getLeft());
  }

  public Stream<R> stream() {
    if (isRight()) {
      return Stream.of(getRight());
    }
    return Stream.empty();
  }
  
  public Option<R> toOption() {
    if (isRight()) {
      return Option.some(getRight());
    }
    return Option.none();
  }
  
  static final class Left<L, R> extends Either<L, R> {
    private L left;
    
    private Left(L value) {
      left = value;
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
      return left;
    }
    
    @Override
    public R getRight() {
      throw new NoSuchElementException("getRight() in left");
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(left);
    }
    
    @Override
    public boolean equals(Object obj) {
      return equal(this)
          .append(comparing(Either::getLeft))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "Left(" + left + ")";
    }
  }

  static final class Right<L, R> extends Either<L, R> {
    private R right;
    
    private Right(R value) {
      right = value;
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
      return right;
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(right);
    }
    
    @Override
    public boolean equals(Object obj) {
      return equal(this)
          .append(comparing(Either::getRight))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "Right(" + right + ")";
    }
  }
}
