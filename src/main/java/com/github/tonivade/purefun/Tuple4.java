/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Equal;

public final class Tuple4<A, B, C, D> implements Tuple, Serializable {

  private static final long serialVersionUID = -2725249702715042810L;

  private final A value1;
  private final B value2;
  private final C value3;
  private final D value4;

  private Tuple4(A value1, B value2, C value3, D value4) {
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
    this.value3 = requireNonNull(value3);
    this.value4 = requireNonNull(value4);
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

  @Override
  public Sequence<Object> toSequence() {
    return Sequence.listOf(value1, value2, value3, value4);
  }

  public <R> Tuple4<R, B, C, D> map1(Function1<A, R> mapper) {
    return Tuple4.of(mapper.apply(value1), value2, value3, value4);
  }

  public <R> Tuple4<A, R, C, D> map2(Function1<B, R> mapper) {
    return Tuple4.of(value1, mapper.apply(value2), value3, value4);
  }

  public <R> Tuple4<A, B, R, D> map3(Function1<C, R> mapper) {
    return Tuple4.of(value1, value2, mapper.apply(value3), value4);
  }

  public <R> Tuple4<A, B, C, R> map4(Function1<D, R> mapper) {
    return Tuple4.of(value1, value2, value3, mapper.apply(value4));
  }

  public <E, F, G, H> Tuple4<E, F, G, H> map(Function1<A, E> map1,
                                             Function1<B, F> map2,
                                             Function1<C, G> map3,
                                             Function1<D, H> map4) {
    return Tuple4.of(map1.apply(value1), map2.apply(value2), map3.apply(value3), map4.apply(value4));
  }

  public static <A, B, C, D> Tuple4<A, B, C, D> of(A value1, B value2, C value3, D value4) {
    return new Tuple4<>(value1, value2, value3, value4);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2, value3, value4);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this)
        .comparing(Tuple4::get1)
        .comparing(Tuple4::get2)
        .comparing(Tuple4::get3)
        .comparing(Tuple4::get4)
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Tuple4(" + value1 + ", " + value2 + ", " + value3 + ", " +  value4 + ")";
  }
}
