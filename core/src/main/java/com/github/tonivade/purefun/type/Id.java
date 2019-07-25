/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Kind;

@HigherKind
public final class Id<T> implements Holder<T>, FlatMap1<Id.µ, T> {

  private final T value;

  private Id(T value) {
    this.value = requireNonNull(value);
  }

  @Override
  public <R> Id<R> map(Function1<T, R> map) {
    return map.andThen(Id::of).apply(value);
  }

  @Override
  public <R> Id<R> flatMap(Function1<T, ? extends Higher1<Id.µ, R>> map) {
    return map.andThen(Id::narrowK).apply(value);
  }

  @Override
  public T get() {
    return value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> Id<V> flatten() {
    try {
      return ((Id<Id<V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
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