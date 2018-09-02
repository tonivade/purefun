/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Higher2;

final class EitherKind<L, R> implements Either<L, R>, Higher2<EitherKind.µ, L, R> {

  public static final class µ {}

  private final Either<L, R> delegate;

  public EitherKind(Either<L, R> delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public boolean isLeft() {
    return delegate.isLeft();
  }

  @Override
  public boolean isRight() {
    return delegate.isRight();
  }

  @Override
  public L getLeft() {
    return delegate.getLeft();
  }

  @Override
  public R getRight() {
    return delegate.getRight();
  }

  public static <L, R> Either<L, R> narrowK(Higher2<EitherKind.µ, L, R> hkt) {
    return (Either<L, R>) hkt;
  }
}
