/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.data.Sequence;

@HigherKind
public final class Tuple2<A, B> implements Tuple, Serializable {

  private static final long serialVersionUID = 5034828839532504174L;

  private static final Equal<Tuple2> EQUAL = Equal.<Tuple2>of()
      .comparing(Tuple2::get1)
      .comparing(Tuple2::get2);

  private final A value1;
  private final B value2;

  private Tuple2(A value1, B value2) {
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
  }

  public A get1() {
    return value1;
  }

  public B get2() {
    return value2;
  }

  @Override
  public Sequence<Object> toSequence() {
    return Sequence.listOf(value1, value2);
  }

  public <C> Tuple2<C, B> map1(Function1<A, C> mapper) {
    return map(mapper, Function1.identity());
  }

  public <C> Tuple2<A, C> map2(Function1<B, C> mapper) {
    return map(Function1.identity(), mapper);
  }

  public <C, D> Tuple2<C, D> map(Function1<A, C> mapper1, Function1<B, D> mapper2) {
    return Tuple2.of(mapper1.apply(value1), mapper2.apply(value2));
  }

  public static <A, B> Tuple2<A, B> of(A value1, B value2) {
    return new Tuple2<>(value1, value2);
  }

  public <R> R applyTo(Function2<A, B, R> function) {
    return function.apply(value1, value2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2);
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "Tuple2(" + value1 + ", " + value2 + ")";
  }
}
