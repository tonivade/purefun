/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.data.ImmutableMap;
import com.github.tonivade.purefun.type.Option;

public interface PartialFunction1<A, R> {

  R apply(A value);

  boolean isDefinedAt(A value);

  default Function1<A, Option<R>> lift() {
    return value -> isDefinedAt(value) ? Option.some(apply(value)) : Option.none();
  }

  default <B> PartialFunction1<A, B> andThen(Function1<R, B> after) {
    return of(value -> after.apply(apply(value)), this::isDefinedAt);
  }

  default <B> Function1<B, R> compose(Function1<B, A> before) {
    return value -> apply(before.apply(value));
  }

  default PartialFunction1<A, R> orElse(PartialFunction1<A, R> other) {
    final PartialFunction1<A, R> self = PartialFunction1.this;
    return of(value -> self.isDefinedAt(value) ? self.apply(value) : other.apply(value),
              value -> self.isDefinedAt(value) || other.isDefinedAt(value));
  }

  default R applyOrElse(A value, Function1<A, R> orElse) {
    if (isDefinedAt(value)) {
      return apply(value);
    }
    return orElse.apply(value);
  }

  static <A, R> PartialFunction1<A, R> of(Function1<A, R> apply, Matcher1<A> isDefined) {
    return new DefaultPartialFunction1<>(apply, isDefined);
  }

  static <R> PartialFunction1<Integer, R> from(ImmutableArray<R> array) {
    return of(array::get, position -> position >= 0 && position < array.size());
  }

  static <K, V> PartialFunction1<K, V> from(ImmutableMap<K, V> map) {
    return of(key -> map.get(key).get(), map::containsKey);
  }
}

class DefaultPartialFunction1<T, R> implements PartialFunction1<T, R> {

  private final Function1<T, R> apply;
  private final Matcher1<T> isDefined;

  DefaultPartialFunction1(Function1<T, R> apply, Matcher1<T> isDefined) {
    this.apply = requireNonNull(apply);
    this.isDefined = requireNonNull(isDefined);
  }

  @Override
  public R apply(T value) {
    return apply.apply(value);
  }

  @Override
  public boolean isDefinedAt(T value) {
    return isDefined.match(value);
  }
}