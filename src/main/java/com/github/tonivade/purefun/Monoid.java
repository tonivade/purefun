/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Producer.unit;

import com.github.tonivade.purefun.data.ImmutableList;

public interface Monoid<T> extends Semigroup<T> {

  T zero();

  static <T> Monoid<T> of(Producer<T> zero, Semigroup<T> combine) {
    return new GenericMonoid<>(zero, combine);
  }
  
  @SuppressWarnings("unchecked")
  static <T> Monoid<ImmutableList<T>> list() {
    return Monoid.class.cast(MonoidInstances.list);
  }
  
  static Monoid<String> string() {
    return MonoidInstances.string;
  }
  
  static Monoid<Integer> integer() {
    return MonoidInstances.integer;
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

interface MonoidInstances {
  @SuppressWarnings("rawtypes")
  Monoid<ImmutableList> list = Monoid.of(ImmutableList::empty, SemigroupInstances.list);
  Monoid<String> string = Monoid.of(unit(""), SemigroupInstances.string);
  Monoid<Integer> integer = Monoid.of(unit(0), SemigroupInstances.integer);
}
