/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.util.Arrays;
import java.util.Objects;

import com.github.tonivade.purefun.Function1;

@FunctionalInterface
public interface Eq<T> {

  boolean eqv(T a, T b);

  default Eq<T> and(Eq<T> other) {
    return (a, b) -> this.eqv(a, b) && other.eqv(a, b);
  }

  static <T> Eq<T> any() {
    return Objects::equals;
  }

  static Eq<Throwable> throwable() {
    return comparing(Throwable::getMessage).and(comparingArray(Throwable::getStackTrace));
  }

  public static <T, V> Eq<T> comparing(Function1<T, V> getter) {
    return (a, b) -> Objects.equals(getter.apply(a), getter.apply(b));
  }

  public static <T, V> Eq<T> comparingArray(Function1<T, V[]> getter) {
    return (a, b) -> Arrays.deepEquals(getter.apply(a), getter.apply(b));
  }
}
