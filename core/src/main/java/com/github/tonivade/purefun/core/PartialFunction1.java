/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.data.ImmutableMap;
import com.github.tonivade.purefun.type.Option;

public interface PartialFunction1<A, R> {

  R apply(A value);

  boolean isDefinedAt(A value);

  default Function1<A, Option<R>> lift() {
    return value -> isDefinedAt(value) ? Option.some(apply(value)) : Option.none();
  }

  default <B> PartialFunction1<A, B> andThen(Function1<? super R, ? extends B> after) {
    return of(this::isDefinedAt, value -> after.apply(apply(value)));
  }

  default <B> Function1<B, R> compose(Function1<? super B, ? extends A> before) {
    return value -> apply(before.apply(value));
  }

  default PartialFunction1<A, R> orElse(PartialFunction1<? super A, ? extends R> other) {
    final PartialFunction1<A, R> self = PartialFunction1.this;
    return of(value -> self.isDefinedAt(value) || other.isDefinedAt(value),
              value -> self.isDefinedAt(value) ? self.apply(value) : other.apply(value));
  }

  default R applyOrElse(A value, Function1<? super A, ? extends R> orElse) {
    if (isDefinedAt(value)) {
      return apply(value);
    }
    return orElse.apply(value);
  }

  static <A, R> PartialFunction1<A, R> of(Matcher1<? super A> isDefined, Function1<? super A, ? extends R> apply) {
    return new PartialFunction1<>() {

      @Override
      public boolean isDefinedAt(A value) {
        return isDefined.match(value);
      }

      @Override
      public R apply(A value) {
        return apply.apply(value);
      }
    };
  }

  static <R> PartialFunction1<Integer, R> from(ImmutableArray<? extends R> array) {
    return of(position -> position >= 0 && position < array.size(), array::get);
  }

  static <K, V> PartialFunction1<K, V> from(ImmutableMap<? super K, ? extends V> map) {
    return of(map::containsKey, key -> map.get(key).getOrElseThrow());
  }
}