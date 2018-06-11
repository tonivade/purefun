/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class Tuple3<A, B, C> {

  private final A value1;
  private final B value2;
  private final C value3;

  private Tuple3(A value1, B value2, C value3) {
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
    this.value3 = requireNonNull(value3);
  }

  public A get1() {
    return value1;
  }

  public B get2() {
    return value2;
  }
  
  public C get3() {
    return value3;
  }

  public static <A, B, C> Tuple3<A, B, C> of(A value1, B value2, C value3) {
    return new Tuple3<A, B, C>(value1, value2, value3);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2, value3);
  }

  @Override
  public boolean equals(Object obj) {
    return equal(this)
        .append(comparing(Tuple3::get1))
        .append(comparing(Tuple3::get2))
        .append(comparing(Tuple3::get3))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Tuple3(" + value1 + ", " + value2 + ", " +  value3 + ")";
  }
}
