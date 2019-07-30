/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Operator2;

@HigherKind
public interface Monoid<T> extends Semigroup<T> {

  T zero();

  default <R> Monoid<R> imap(Function1<T, R> map, Function1<R, T> comap) {
    return new MonoidInvariant() {}.imap(this, map, comap);
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

@Instance
interface MonoidInvariant extends Invariant<Monoid.µ> {

  @Override
  default <A, B> Monoid<B> imap(Higher1<Monoid.µ, A> value,
                                Function1<A, B> map,
                                Function1<B, A> comap) {
    return new Monoid<B>() {

      @Override
      public B zero() {
        return map.apply(value.fix1(Monoid::narrowK).zero());
      }

      @Override
      public B combine(B t1, B t2) {
        return map.apply(value.fix1(Monoid::narrowK).combine(comap.apply(t1), comap.apply(t2)));
      }
    };
  }
}
