/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Function1.identity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind2;
import com.github.tonivade.purefun.data.Sequence;

@HigherKind
public final class Tuple2<A, B> implements Tuple, Tuple2Of<A, B>, Kind2<Tuple2<?, ?>, A, B>, Serializable {

  @Serial
  private static final long serialVersionUID = 5034828839532504174L;

  private static final Equal<Tuple2<?, ?>> EQUAL = Equal.<Tuple2<?, ?>>of()
      .comparing(Tuple2::get1)
      .comparing(Tuple2::get2);

  private final A value1;
  private final B value2;

  private Tuple2(A value1, B value2) {
    this.value1 = value1;
    this.value2 = value2;
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

  public <C> Tuple2<C, B> map1(Function1<? super A, ? extends C> mapper) {
    return map(mapper, identity());
  }

  public <C> Tuple2<A, C> map2(Function1<? super B, ? extends C> mapper) {
    return map(identity(), mapper);
  }

  public <C, D> Tuple2<C, D> map(Function1<? super A, ? extends C> mapper1, Function1<? super B, ? extends D> mapper2) {
    return Tuple2.of(mapper1.apply(value1), mapper2.apply(value2));
  }

  public static <A, B> Tuple2<A, B> of(A value1, B value2) {
    return new Tuple2<>(value1, value2);
  }

  public <R> R applyTo(Function2<? super A, ? super B, ? extends R> function) {
    return function.apply(value1, value2);
  }

  public void consume(Consumer2<? super A, ? super B> consumer) {
    consumer.accept(value1, value2);
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
