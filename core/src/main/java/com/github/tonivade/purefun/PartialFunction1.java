/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

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
    return of(this::isDefinedAt, value -> after.apply(apply(value)));
  }

  default <B> Function1<B, R> compose(Function1<B, A> before) {
    return value -> apply(before.apply(value));
  }

  default PartialFunction1<A, R> orElse(PartialFunction1<A, R> other) {
    final PartialFunction1<A, R> self = PartialFunction1.this;
    return of(value -> self.isDefinedAt(value) || other.isDefinedAt(value),
              value -> self.isDefinedAt(value) ? self.apply(value) : other.apply(value));
  }

  default R applyOrElse(A value, Function1<A, R> orElse) {
    if (isDefinedAt(value)) {
      return apply(value);
    }
    return orElse.apply(value);
  }

  static <A, R> PartialFunction1<A, R> of(Matcher1<A> isDefined, Function1<A, R> apply) {
    return new PartialFunction1<A, R>() {

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

  static <R> PartialFunction1<Integer, R> from(ImmutableArray<R> array) {
    return of(position -> position >= 0 && position < array.size(), array::get);
  }

  static <K, V> PartialFunction1<K, V> from(ImmutableMap<K, V> map) {
    return of(map::containsKey, key -> map.get(key).get());
  }
}