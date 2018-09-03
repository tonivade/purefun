/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Monoid<T> extends Semigroup<T> {

  T zero();

  static <T> Monoid<T> monoid(Producer<T> zero, Semigroup<T> combine) {
    return new GenericMonoid<>(zero, combine);
  }

  final class GenericMonoid<T> implements Monoid<T> {
    private final Producer<T> zero;
    private final Semigroup<T> combine;

    private GenericMonoid(Producer<T> zero, Semigroup<T> combine) {
      this.zero = zero;
      this.combine = combine;
    }

    @Override
    public T zero() {
      return zero.get();
    }

    @Override
    public T combine(T t1, T t2) {
      return combine.combine(t1, t2);
    }
  }
}
