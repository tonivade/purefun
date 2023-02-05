/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Bindable;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.typeclasses.Monoid;

@HigherKind
public non-sealed interface Writer<L, A> extends WriterOf<L, A>, Bindable<Kind<Writer_, L>, A> {

  Monoid<L> monoid();
  Tuple2<L, A> value();

  default A getValue() {
    return value().get2();
  }

  default L getLog() {
    return value().get1();
  }

  @Override
  default <B> Writer<L, B> map(Function1<? super A, ? extends B> mapper) {
    return bimap(monoid(), identity(), mapper);
  }

  default <R> Writer<R, A> mapLog(Monoid<R> monoidR, Function1<? super L, ? extends R> mapper) {
    return bimap(monoidR, mapper, identity());
  }

  default Writer<L, A> append(L log2) {
    return mapLog(monoid(), log1 -> monoid().combine(log1, log2));
  }

  default Writer<L, A> reset() {
    return mapLog(monoid(), cons(monoid().zero()));
  }

  default <V, R> Writer<V, R> bimap(Monoid<V> monoidV,
                                    Function1<? super L, ? extends V> mapper1,
                                    Function1<? super A, ? extends R> mapper2) {
    return writer(monoidV, value().map(mapper1, mapper2));
  }

  @Override
  default <B> Writer<L, B> flatMap(Function1<? super A, ? extends Kind<Kind<Writer_, L>, ? extends B>> mapper) {
    Writer<L, B> apply = mapper.andThen(WriterOf::<L, B>narrowK).apply(value().get2());
    Tuple2<L, A> combine = value().map1(log -> monoid().combine(log, apply.getLog()));
    return writer(monoid(), Tuple.of(combine.get1(), apply.getValue()));
  }

  static <L, A> Writer<L, A> pure(Monoid<L> monoid, A value) {
    return writer(monoid, Tuple.of(monoid.zero(), value));
  }

  static <L, A> Writer<L, A> writer(Monoid<L> monoid, Tuple2<L, A> value) {
    return new Writer<>() {

      @Override
      public Monoid<L> monoid() { return monoid; }

      @Override
      public Tuple2<L, A> value() { return value; }
    };
  }
}
