/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Producer;

public interface Monoid<T> extends Semigroup<T> {

  T zero();

  static <T> Monoid<T> of(Producer<T> zero, Semigroup<T> combine) {
    return new GenericMonoid<>(zero, combine);
  }
}

class GenericMonoid<T> implements Monoid<T> {
  private final Producer<T> zero;
  private final Semigroup<T> semigroup;

  GenericMonoid(Producer<T> zero, Semigroup<T> semigroup) {
    this.zero = requireNonNull(zero);
    this.semigroup = requireNonNull(semigroup);
  }

  @Override
  public T zero() {
    return zero.get();
  }

  @Override
  public T combine(T t1, T t2) {
    return semigroup.combine(t1, t2);
  }
}