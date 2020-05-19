/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Operator2;

@HigherKind
public interface Monoid<T> extends MonoidOf<T>, Semigroup<T> {

  T zero();

  default <R> Monoid<R> imap(Function1<T, R> map, Function1<R, T> comap) {
    return MonoidInvariant.INSTANCE.imap(this, map, comap).fix1(MonoidOf::<R>narrowK);
  }

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

interface MonoidInvariant extends Invariant<Monoid_> {

  MonoidInvariant INSTANCE = new MonoidInvariant() { };

  @Override
  default <A, B> Higher1<Monoid_, B> imap(Higher1<Monoid_, A> value,
                                           Function1<A, B> map,
                                           Function1<B, A> comap) {
    return new Monoid<B>() {

      @Override
      public B zero() {
        return map.apply(value.fix1(MonoidOf::narrowK).zero());
      }

      @Override
      public B combine(B t1, B t2) {
        return map.apply(value.fix1(MonoidOf::narrowK).combine(comap.apply(t1), comap.apply(t2)));
      }
    };
  }
}
