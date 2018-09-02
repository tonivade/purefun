/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Higher;

final class TryKind<T> implements Try<T>, Higher<TryKind.µ, T> {

  public static final class µ {}

  private final Try<T> delegate;

  public TryKind(Try<T> delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public T get() {
    return delegate.get();
  }

  @Override
  public Throwable getCause() {
    return delegate.getCause();
  }

  @Override
  public boolean isSuccess() {
    return delegate.isSuccess();
  }

  @Override
  public boolean isFailure() {
    return delegate.isFailure();
  }

  @Override
  public <V> Try<V> flatten() {
    return delegate.flatten();
  }

  public static <T> Try<T> narrowK(Higher<TryKind.µ, T> hkt) {
    return (Try<T>) hkt;
  }
}
