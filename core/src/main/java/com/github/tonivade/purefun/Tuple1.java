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
public final class Tuple1<A> implements Tuple, Serializable {

  private static final long serialVersionUID = 6343431593011527978L;

  private final A value1;

  private Tuple1(A value1) {
    this.value1 = requireNonNull(value1);
  }

  public A get1() {
    return value1;
  }

  @Override
  public Sequence<Object> toSequence() {
    return Sequence.listOf(value1);
  }

  public <B> Tuple1<B> map(Function1<A, B> mapper) {
    return new Tuple1<>(mapper.apply(value1));
  }

  public static <A> Tuple1<A> of(A value1) {
    return new Tuple1<>(value1);
  }

  public <R> R applyTo(Function1<A, R> function) {
    return function.apply(value1);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this)
        .comparing(Tuple1::get1)
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Tuple1(" + value1 + ")";
  }
}
