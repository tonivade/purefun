/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Operator2;

@FunctionalInterface
public interface Semigroup<T> {

  T combine(T t1, T t2);

  static <T> Semigroup<T> of(Operator2<T> combine) {
    return combine::apply;
  }
}
