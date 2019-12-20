/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.HigherKind;

@HigherKind
public final class Const<T, A> implements Serializable {

  private static final long serialVersionUID = 7431389527943145565L;

  private static final Equal<Const<?, ?>> EQUAL = Equal.<Const<?, ?>>of().comparing(Const::get);

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

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "Const(" + value + ")";
  }

  public static <T, A> Const<T, A> of(T value) {
    return new Const<>(value);
  }
}
