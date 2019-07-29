/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Operator2;

@HigherKind
public interface Monoid<T> extends Semigroup<T> {

  T zero();

  static Monoid<String> string() {
    return Monoid.of("", (a, b) -> a + b);
  }

  static Monoid<Integer> integer() {
    return Monoid.of(0, (a, b) -> a + b);
  }

  static <T> Monoid<T> of(T zero, Operator2<T> combinator) {
    return new Monoid<T>() {

      @Override
      public T zero() {
        return zero;
      }

      @Override
      public T combine(T t1, T t2) {
        return combinator.apply(t1, t2);
      }
    };
  }
}
