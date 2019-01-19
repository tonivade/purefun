/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;

public final class Writer<L, A> implements FlatMap2<Writer.µ, L, A> {

  public static final class µ implements Kind {}

  private final Monoid<L> monoid;
  private final Tuple2<L, A> value;

  private Writer(Monoid<L> monoid, Tuple2<L, A> value) {
    this.monoid = requireNonNull(monoid);
    this.value = requireNonNull(value);
  }

  public A getValue() {
    return value.get2();
  }

  public L getLog() {
    return value.get1();
  }

  @Override
  public <B> Writer<L, B> map(Function1<A, B> mapper) {
    return bimap(monoid, identity(), mapper);
  }

  public <R> Writer<R, A> mapLog(Monoid<R> monoidR, Function1<L, R> mapper) {
    return bimap(monoidR, mapper, identity());
  }

  public Writer<L, A> append(L log2) {
    return mapLog(monoid, log1 -> monoid.combine(log1, log2));
  }

  public Writer<L, A> reset() {
    return mapLog(monoid, cons(monoid.zero()));
  }

  public <V, R> Writer<V, R> bimap(Monoid<V> monoidV, Function1<L, V> mapper1, Function1<A, R> mapper2) {
    return new Writer<>(monoidV, value.map(mapper1, mapper2));
  }

  @Override
  public <B> Writer<L, B> flatMap(Function1<A, ? extends Higher2<Writer.µ, L, B>> mapper) {
    Writer<L, B> apply = mapper.andThen(Writer::narrowK).apply(value.get2());
    Tuple2<L, A> combine = value.map1(log -> monoid.combine(log, apply.getLog()));
    return writer(monoid, combine.get1(), apply.getValue());
  }

  public static <L, A> Writer<L, A> pure(Monoid<L> monoid, A value) {
    return writer(monoid, monoid.zero(), value);
  }

  public static <L, A> Writer<L, A> writer(Monoid<L> monoid, L log, A value) {
    return new Writer<>(monoid, Tuple.of(log, value));
  }

  public static <T, A> Writer<Sequence<T>, A> listPure(A value) {
    return pure(Sequence.monoid(), value);
  }

  public static <T, A> Writer<Sequence<T>, A> listWriter(T log, A value) {
    return writer(Sequence.monoid(), listOf(log), value);
  }

  public static <L> Monad<Higher1<Writer.µ, L>> monad(Monoid<L> monoid) {
    return new Monad<Higher1<Writer.µ, L>>() {

      @Override
      public <T> Writer<L, T> pure(T value) {
        return Writer.pure(monoid, value);
      }

      @Override
      public <T, R> Writer<L, R> flatMap(Higher1<Higher1<Writer.µ, L>, T> value,
          Function1<T, ? extends Higher1<Higher1<Writer.µ, L>, R>> map) {
        return Writer.narrowK(value).flatMap(map.andThen(Writer::narrowK));
      }
    };
  }

  public static <L, T> Writer<L, T> narrowK(Higher2<Writer.µ, L, T> hkt) {
    return (Writer<L, T>) hkt;
  }

  public static <L, T> Writer<L, T> narrowK(Higher1<Higher1<Writer.µ, L>, T> hkt) {
    return (Writer<L, T>) hkt;
  }
}
