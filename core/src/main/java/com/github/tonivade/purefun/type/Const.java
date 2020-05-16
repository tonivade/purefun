/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.HigherKind;

@HigherKind
public final class Const<T, A> implements Higher2<Const_, T, A>, Serializable {

  private static final long serialVersionUID = 7431389527943145565L;

  private static final Equal<Const<?, ?>> EQUAL = Equal.<Const<?, ?>>of().comparing(Const::get);

  private final T value;

  private Const(T value) {
    this.value = checkNonNull(value);
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
