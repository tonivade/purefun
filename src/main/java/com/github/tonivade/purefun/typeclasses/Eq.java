/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.util.Arrays;
import java.util.Objects;

public interface Eq<T> {

  boolean eqv(T a, T b);

  static <T> Eq<T> object() {
    return Objects::equals;
  }

  static Eq<Throwable> throwable() {
    return (a, b) -> Objects.equals(a.getMessage(), b.getMessage())
        && Arrays.deepEquals(a.getStackTrace(), b.getStackTrace());
  }
}
