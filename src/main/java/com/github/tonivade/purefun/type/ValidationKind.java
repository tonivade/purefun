/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Higher2;

public class ValidationKind<E, T> implements Validation<E, T>, Higher2<ValidationKind.µ, E, T> {

  public static final class µ {}

  private final Validation<E, T> delegate;

  public ValidationKind(Validation<E, T> delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public T get() {
    return delegate.get();
  }

  @Override
  public boolean isValid() {
    return delegate.isValid();
  }

  @Override
  public boolean isInvalid() {
    return delegate.isInvalid();
  }

  @Override
  public E getError() {
    return delegate.getError();
  }

  public static <E, T> Validation<E, T> narrowK(Higher2<ValidationKind.µ, E, T> hkt) {
    return (Validation<E, T>) hkt;
  }
}
