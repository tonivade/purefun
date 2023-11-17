/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.annotation.HigherKind;

@HigherKind
public record Const<T, A>(T value) implements ConstOf<T, A> {

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
