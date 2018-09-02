/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Higher;

public final class OptionKind<T> implements Option<T>, Higher<OptionKind.µ, T> {

  public static final class µ {}

  private final Option<T> delegate;

  public OptionKind(Option<T> delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public T get() {
    return delegate.get();
  }

  @Override
  public boolean isPresent() {
    return delegate.isPresent();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public <V> Option<V> flatten() {
    return delegate.flatten();
  }

  public static <T> Option<T> narrowK(Higher<OptionKind.µ, T> hkt) {
    return (Option<T>) hkt;
  }
}
