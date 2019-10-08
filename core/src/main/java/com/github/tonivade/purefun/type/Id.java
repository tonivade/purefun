/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;

/**
 * <p>This is the identity monad. It only wraps the value and nothing more.</p>
 * <p>You can go from {@code T} to {@code Id<T>} and from {@code Id<T>} to {@code T}
 * without loosing information.</p>
 * @param <T> the wrapped value
 */
@HigherKind
public final class Id<T> implements Serializable {

  private static final long serialVersionUID = -6295106408421985189L;

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