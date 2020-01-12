/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.data.Sequence;

public final class Tuple3<A, B, C> implements Tuple, Serializable {

  private static final long serialVersionUID = -4316365232845710129L;

  private static final Equal<Tuple3<?, ?, ?>> EQUAL = Equal.<Tuple3<?, ?, ?>>of()
      .comparing(Tuple3::get1)
      .comparing(Tuple3::get2)
      .comparing(Tuple3::get3);

  private final A value1;
  private final B value2;
  private final C value3;

  private Tuple3(A value1, B value2, C value3) {
    this.value1 = value1;
    this.value2 = value2;
    this.value3 = value3;
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

  @Override
  public Sequence<Object> toSequence() {
    return Sequence.listOf(value1, value2, value3);
  }

  public <R> Tuple3<R, B, C> map1(Function1<A, R> mapper) {
    return Tuple3.of(mapper.apply(value1), value2, value3);
  }

  public <R> Tuple3<A, R, C> map2(Function1<B, R> mapper) {
    return Tuple3.of(value1, mapper.apply(value2), value3);
  }

  public <R> Tuple3<A, B, R> map3(Function1<C, R> mapper) {
    return Tuple3.of(value1, value2, mapper.apply(value3));
  }

  public <D, E, F> Tuple3<D, E, F> map(Function1<A, D> map1, Function1<B, E> map2, Function1<C, F> map3) {
    return Tuple3.of(map1.apply(value1), map2.apply(value2), map3.apply(value3));
  }

  public <R> R applyTo(Function3<A, B, C, R> function) {
    return function.apply(value1, value2, value3);
  }

  public static <A, B, C> Tuple3<A, B, C> of(A value1, B value2, C value3) {
    return new Tuple3<>(value1, value2, value3);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2, value3);
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "Tuple3(" + value1 + ", " + value2 + ", " +  value3 + ")";
  }
}
