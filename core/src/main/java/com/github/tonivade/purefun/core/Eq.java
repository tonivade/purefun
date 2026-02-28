/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import java.util.Arrays;
import java.util.Objects;

import com.github.tonivade.purefun.type.Option;

/**
 * <p>This interface represents a function that verify if two instances of a type are equivalent.</p>
 * <p>{@code Eq} instances can be composed using {@code and()} method</p>
 * @param <T> type to verify
 */
@FunctionalInterface
public interface Eq<T> {

  boolean eqv(T a, T b);

  default Eq<T> and(Eq<T> other) {
    return (a, b) -> this.eqv(a, b) && other.eqv(a, b);
  }

  static <T> Eq<T> any() {
    return Objects::equals;
  }

  static <T> Eq<T> always() {
    return (a, b) -> true;
  }

  static Eq<Throwable> throwable() {
    return comparing((Throwable t) -> Option.of(t.getMessage()))
        .and(comparingArray(Throwable::getStackTrace));
  }

  static <T, V> Eq<T> comparing(Function1<? super T, ? extends V> getter) {
    return (a, b) -> Objects.equals(getter.apply(a), getter.apply(b));
  }

  static <T, V> Eq<T> comparingArray(Function1<? super T, ? extends V[]> getter) {
    return (a, b) -> Arrays.deepEquals(getter.apply(a), getter.apply(b));
  }
}
