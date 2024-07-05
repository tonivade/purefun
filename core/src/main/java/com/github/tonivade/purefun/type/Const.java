/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.io.Serializable;

import com.github.tonivade.purefun.Kind;

public record Const<T, A>(T value) implements Kind<Const<T, ?>, A>, Serializable {

  public Const {
    checkNonNull(value);
  }

  @SuppressWarnings("unchecked")
  public <B> Const<T, B> retag() {
    return (Const<T, B>) this;
  }

  @Override
  public String toString() {
    return "Const(" + value + ")";
  }

  public static <T, A> Const<T, A> of(T value) {
    return new Const<>(value);
  }
}
