/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class Tuple4<A, B, C, D> {

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
  
  public <R> Tuple4<R, B, C, D> map1(Handler1<A, R> mapper) {
    return Tuple4.of(mapper.handle(value1), value2, value3, value4);
  }
  
  public <R> Tuple4<A, R, C, D> map2(Handler1<B, R> mapper) {
    return Tuple4.of(value1, mapper.handle(value2), value3, value4);
  }
  
  public <R> Tuple4<A, B, R, D> map3(Handler1<C, R> mapper) {
    return Tuple4.of(value1, value2, mapper.handle(value3), value4);
  }
  
  public <R> Tuple4<A, B, C, R> map4(Handler1<D, R> mapper) {
    return Tuple4.of(value1, value2, value3, mapper.handle(value4));
  }
  
  public <E, F, G, H> Tuple4<E, F, G, H> map(Handler1<A, E> map1, 
                                             Handler1<B, F> map2, 
                                             Handler1<C, G> map3, 
                                             Handler1<D, H> map4) {
    return Tuple4.of(map1.handle(value1), map2.handle(value2), map3.handle(value3), map4.handle(value4));
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
    return equal(this)
        .append(comparing(Tuple4::get1))
        .append(comparing(Tuple4::get2))
        .append(comparing(Tuple4::get3))
        .append(comparing(Tuple4::get4))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Tuple4(" + value1 + ", " + value2 + ", " + value3 + ", " +  value4 + ")";
  }
}
