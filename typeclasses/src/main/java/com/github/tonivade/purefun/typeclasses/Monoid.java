/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.MonoidOf.toMonoid;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Operator2;
import com.github.tonivade.purefun.HigherKind;

@HigherKind
public non-sealed interface Monoid<T> extends MonoidOf<T>, Semigroup<T> {

  T zero();

  default <R> Monoid<R> imap(Function1<T, R> map, Function1<R, T> comap) {
    return MonoidInvariant.INSTANCE.imap(this, map, comap).fix(MonoidOf::narrowK);
  }

  static Monoid<String> string() {
    return Monoid.of("", (a, b) -> a + b);
  }

  static Monoid<Integer> integer() {
    return Monoid.of(0, Integer::sum);
  }

  static <T> Monoid<T> of(T zero, Operator2<T> combinator) {
    return new Monoid<>() {

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

interface MonoidInvariant extends Invariant<Monoid<?>> {

  MonoidInvariant INSTANCE = new MonoidInvariant() { };

  @Override
  default <A, B> Kind<Monoid<?>, B> imap(Kind<Monoid<?>, ? extends A> value,
                                       Function1<? super A, ? extends B> map,
                                       Function1<? super B, ? extends A> comap) {
    return new Monoid<>() {

      @Override
      public B zero() {
        return map.apply(value.fix(toMonoid()).zero());
      }

      @Override
      public B combine(B t1, B t2) {
        return map.apply(value.fix(MonoidOf::<A>narrowK).combine(comap.apply(t1), comap.apply(t2)));
      }
    };
  }
}
