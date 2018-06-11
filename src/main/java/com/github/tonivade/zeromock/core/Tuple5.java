/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class Tuple5<A, B, C, D, E> {

  private final A value1;
  private final B value2;
  private final C value3;
  private final D value4;
  private final E value5;

  private Tuple5(A value1, B value2, C value3, D value4, E value5) {
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
    this.value3 = requireNonNull(value3);
    this.value4 = requireNonNull(value4);
    this.value5 = requireNonNull(value5);
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
  
  public D get4() {
    return value4;
  }
  
  public E get5() {
    return value5;
  }

  public static <A, B, C, D, E> Tuple5<A, B, C, D, E> of(A value1, B value2, C value3, D value4, E value5) {
    return new Tuple5<A, B, C, D, E>(value1, value2, value3, value4, value5);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2, value3, value4, value5);
  }

  @Override
  public boolean equals(Object obj) {
    return equal(this)
        .append(comparing(Tuple5::get1))
        .append(comparing(Tuple5::get2))
        .append(comparing(Tuple5::get3))
        .append(comparing(Tuple5::get4))
        .append(comparing(Tuple5::get5))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Tuple5(" + value1 + ", " + value2 + ", " + value3 + ", " + value4 + ", " +  value5 + ")";
  }
}
