/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class With<A> {

  private final A value;

  private With(A value) {
    this.value = requireNonNull(value);
  }

  public A get() {
    return value;
  }

  public void end(Consumer1<A> consumer) {
     consumer.accept(value);
  }

  public <R> With<R> then(Function1<A, R> function) {
    return with(function.apply(value));
  }

  public <B, R> With<R> then(Function2<A, B, R> function, B b) {
    return with(function.apply(value, b));
  }

  public <B, C, R> With<R> then(Function3<A, B, C, R> function, B b, C c) {
    return with(function.apply(value, b, c));
  }

  public <B, C, D, R> With<R> then(Function4<A, B, C, D, R> function, B b, C c, D d) {
    return with(function.apply(value, b, c, d));
  }

  public <B, C, D, E, R> With<R> then(Function5<A, B, C, D, E, R> function, B b, C c, D d, E e) {
    return with(function.apply(value, b, c, d, e));
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this).comparing(With::get).applyTo(obj);
  }

  public static <T> With<T> with(T value) {
    return new With<>(value);
  }
}
