/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.data.Sequence;

public final class Tuple4<A, B, C, D> implements Tuple, Serializable {

  @Serial
  private static final long serialVersionUID = -2725249702715042810L;

  private static final Equal<Tuple4<?, ?, ?, ?>> EQUAL = Equal.<Tuple4<?, ?, ?, ?>>of()
      .comparing(Tuple4::get1)
      .comparing(Tuple4::get2)
      .comparing(Tuple4::get3)
      .comparing(Tuple4::get4);

  private final A value1;
  private final B value2;
  private final C value3;
  private final D value4;

  private Tuple4(A value1, B value2, C value3, D value4) {
    this.value1 = value1;
    this.value2 = value2;
    this.value3 = value3;
    this.value4 = value4;
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

  public <R> Tuple4<R, B, C, D> map1(Function1<? super A, ? extends R> mapper) {
    return Tuple4.of(mapper.apply(value1), value2, value3, value4);
  }

  public <R> Tuple4<A, R, C, D> map2(Function1<? super B, ? extends R> mapper) {
    return Tuple4.of(value1, mapper.apply(value2), value3, value4);
  }

  public <R> Tuple4<A, B, R, D> map3(Function1<? super C, ? extends R> mapper) {
    return Tuple4.of(value1, value2, mapper.apply(value3), value4);
  }

  public <R> Tuple4<A, B, C, R> map4(Function1<? super D, ? extends R> mapper) {
    return Tuple4.of(value1, value2, value3, mapper.apply(value4));
  }

  public <E, F, G, H> Tuple4<E, F, G, H> map(Function1<? super A, ? extends E> map1,
                                             Function1<? super B, ? extends F> map2,
                                             Function1<? super C, ? extends G> map3,
                                             Function1<? super D, ? extends H> map4) {
    return Tuple4.of(map1.apply(value1), map2.apply(value2), map3.apply(value3), map4.apply(value4));
  }

  public <R> R applyTo(Function4<? super A, ? super B, ? super C, ? super D, ? extends R> function) {
    return function.apply(value1, value2, value3, value4);
  }

  public void consume(Consumer4<? super A, ? super B, ? super C, ? super D> consumer) {
    consumer.accept(value1, value2, value3, value4);
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
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "Tuple4(" + value1 + ", " + value2 + ", " + value3 + ", " +  value4 + ")";
  }
}
