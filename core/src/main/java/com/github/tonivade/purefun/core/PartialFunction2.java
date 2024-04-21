/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import com.github.tonivade.purefun.type.Option;

public interface PartialFunction2<A, B, R> {

  R apply(A a, B b);

  boolean isDefinedAt(A a, B b);

  default Function2<A, B, Option<R>> lift() {
    return (a, b) -> isDefinedAt(a, b) ? Option.some(apply(a, b)) : Option.none();
  }

  static <A, B, R> PartialFunction2<A, B, R> of(
      Matcher2<? super A, ? super B> isDefined, 
      Function2<? super A, ? super B, ? extends R> apply) {
    return new PartialFunction2<>() {

      @Override
      public boolean isDefinedAt(A a, B b) {
        return isDefined.match(a, b);
      }

      @Override
      public R apply(A a, B b) {
        return apply.apply(a, b);
      }
    };
  }
}
