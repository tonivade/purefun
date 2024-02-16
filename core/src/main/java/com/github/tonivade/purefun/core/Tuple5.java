/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.data.Sequence;

public final class Tuple5<A, B, C, D, E> implements Tuple, Serializable {

  @Serial
  private static final long serialVersionUID = 4097431156050938896L;

  private static final Equal<Tuple5<?, ?, ?, ?, ?>> EQUAL = Equal.<Tuple5<?, ?, ?, ?, ?>>of()
      .comparing(Tuple5::get1)
      .comparing(Tuple5::get2)
      .comparing(Tuple5::get3)
      .comparing(Tuple5::get4)
      .comparing(Tuple5::get5);

  private final A value1;
  private final B value2;
  private final C value3;
  private final D value4;
  private final E value5;

  private Tuple5(A value1, B value2, C value3, D value4, E value5) {
    this.value1 = value1;
    this.value2 = value2;
    this.value3 = value3;
    this.value4 = value4;
    this.value5 = value5;
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

  public <R> Tuple5<R, B, C, D, E> map1(Function1<? super A, ? extends R> mapper) {
    return Tuple5.of(mapper.apply(value1), value2, value3, value4, value5);
  }

  public <R> Tuple5<A, R, C, D, E> map2(Function1<? super B, ? extends R> mapper) {
    return Tuple5.of(value1, mapper.apply(value2), value3, value4, value5);
  }

  public <R> Tuple5<A, B, R, D, E> map3(Function1<? super C, ? extends R> mapper) {
    return Tuple5.of(value1, value2, mapper.apply(value3), value4, value5);
  }

  public <R> Tuple5<A, B, C, R, E> map4(Function1<? super D, ? extends R> mapper) {
    return Tuple5.of(value1, value2, value3, mapper.apply(value4), value5);
  }

  public <R> Tuple5<A, B, C, D, R> map5(Function1<? super E, ? extends R> mapper) {
    return Tuple5.of(value1, value2, value3, value4, mapper.apply(value5));
  }

  public <F, G, H, I, J> Tuple5<F, G, H, I, J> map(Function1<? super A, ? extends F> map1,
                                                   Function1<? super B, ? extends G> map2,
                                                   Function1<? super C, ? extends H> map3,
                                                   Function1<? super D, ? extends I> map4,
                                                   Function1<? super E, ? extends J> map5) {
    return Tuple5.of(map1.apply(value1), map2.apply(value2), map3.apply(value3), map4.apply(value4), map5.apply(value5));
  }

  public <R> R applyTo(Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> function) {
    return function.apply(value1, value2, value3, value4, value5);
  }

  public void consume(Consumer5<? super A, ? super B, ? super C, ? super D, ? super E> consumer) {
    consumer.accept(value1, value2, value3, value4, value5);
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
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "Tuple5(" + value1 + ", " + value2 + ", " + value3 + ", " + value4 + ", " +  value5 + ")";
  }
}
