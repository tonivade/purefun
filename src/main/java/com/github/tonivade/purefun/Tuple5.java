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

public final class Tuple5<A, B, C, D, E> implements Tuple, Serializable {

  private static final long serialVersionUID = 4097431156050938896L;

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

  @Override
  public Sequence<Object> toSequence() {
    return Sequence.listOf(value1, value2, value3, value4, value5);
  }

  public <R> Tuple5<R, B, C, D, E> map1(Function1<A, R> mapper) {
    return Tuple5.of(mapper.apply(value1), value2, value3, value4, value5);
  }

  public <R> Tuple5<A, R, C, D, E> map2(Function1<B, R> mapper) {
    return Tuple5.of(value1, mapper.apply(value2), value3, value4, value5);
  }

  public <R> Tuple5<A, B, R, D, E> map3(Function1<C, R> mapper) {
    return Tuple5.of(value1, value2, mapper.apply(value3), value4, value5);
  }

  public <R> Tuple5<A, B, C, R, E> map4(Function1<D, R> mapper) {
    return Tuple5.of(value1, value2, value3, mapper.apply(value4), value5);
  }

  public <R> Tuple5<A, B, C, D, R> map5(Function1<E, R> mapper) {
    return Tuple5.of(value1, value2, value3, value4, mapper.apply(value5));
  }

  public <F, G, H, I, J> Tuple5<F, G, H, I, J> map(Function1<A, F> map1,
                                                   Function1<B, G> map2,
                                                   Function1<C, H> map3,
                                                   Function1<D, I> map4,
                                                   Function1<E, J> map5) {
    return Tuple5.of(map1.apply(value1), map2.apply(value2), map3.apply(value3), map4.apply(value4), map5.apply(value5));
  }

  public static <A, B, C, D, E> Tuple5<A, B, C, D, E> of(A value1, B value2, C value3, D value4, E value5) {
    return new Tuple5<>(value1, value2, value3, value4, value5);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2, value3, value4, value5);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this)
        .comparing(Tuple5::get1)
        .comparing(Tuple5::get2)
        .comparing(Tuple5::get3)
        .comparing(Tuple5::get4)
        .comparing(Tuple5::get5)
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Tuple5(" + value1 + ", " + value2 + ", " + value3 + ", " + value4 + ", " +  value5 + ")";
  }
}
