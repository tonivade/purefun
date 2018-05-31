/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Objects;

public final class Tupple2<T, U> {

  private final T value1;
  private final U value2;

  private Tupple2(T value1, U value2) {
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
  }

  public T get1() {
    return value1;
  }

  public U get2() {
    return value2;
  }

  public static <T, U> Tupple2<T, U> of(T value1, U value2) {
    return new Tupple2<T, U>(value1, value2);
  }

  public static <T, U> Tupple2<T, U> from(Map.Entry<T, U> entry) {
    return new Tupple2<T, U>(entry.getKey(), entry.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2);
  }

  @Override
  public boolean equals(Object obj) {
    return equal(this)
        .append(comparing(Tupple2::get1))
        .append(comparing(Tupple2::get2))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Tupple2(" + value1 + ", " + value2 + ")";
  }
}
