/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.data.ImmutableList;

@FunctionalInterface
public interface Semigroup<T> {

  T combine(T t1, T t2);
  
  static <T> Semigroup<T> semigroup(Operator2<T> combine) {
    return combine::apply;
  }
  
  @SuppressWarnings("unchecked")
  static <T> Semigroup<ImmutableList<T>> list() {
    return Semigroup.class.cast(SemigroupInstances.list);
  }
  
  static Semigroup<String> string() {
    return SemigroupInstances.string;
  }
  
  static Semigroup<Integer> integer() {
    return SemigroupInstances.integer;
  }
}

interface SemigroupInstances {
  @SuppressWarnings({ "rawtypes", "unchecked" })
  Semigroup<ImmutableList> list = ImmutableList::appendAll;
  Semigroup<String> string = String::concat;
  Semigroup<Integer> integer = (a, b) -> a + b;
}
