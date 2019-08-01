/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;

@HigherKind
public final class Id<T> {

  private final T value;

  private Id(T value) {
    this.value = requireNonNull(value);
  }

  public <R> Id<R> map(Function1<T, R> map) {
    return map.andThen(Id::of).apply(value);
  }

  public <R> Id<R> flatMap(Function1<T, Id<R>> map) {
    return map.apply(value);
  }

  public T get() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this).comparing(Id::get).applyTo(obj);
  }

  @Override
  public String toString() {
    return "Id(" + value + ")";
  }

  public static <T> Id<T> of(T value) {
    return new Id<>(value);
  }
}