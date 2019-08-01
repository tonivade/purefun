/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.HigherKind;

@HigherKind
public final class Const<T, A> {

  private final T value;

  private Const(T value) {
    this.value = requireNonNull(value);
  }

  public T get() {
    return value;
  }

  @SuppressWarnings("unchecked")
  public <B> Const<T, B> retag() {
    return (Const<T, B>) this;
  }

  public static <T, A> Const<T, A> of(T value) {
    return new Const<>(value);
  }
}
