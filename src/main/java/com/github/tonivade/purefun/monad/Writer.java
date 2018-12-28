/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Monoid;

public final class Writer<L, A> implements FlatMap2<Writer.µ, L, A> {

  public static final class µ implements Kind {}

  private final Monoid<L> monoid;
  private final A value;
  private final L log;

  private Writer(Monoid<L> monoid, L log, A value) {
    this.monoid = requireNonNull(monoid);
    this.log = requireNonNull(log);
    this.value = requireNonNull(value);
  }

  public A getValue() {
    return value;
  }

  public L getLog() {
    return log;
  }

  @Override
  public <B> Writer<L, B> map(Function1<A, B> map) {
    return new Writer<>(monoid, log, map.apply(value));
  }

  @Override
  public <B> Writer<L, B> flatMap(Function1<A, ? extends Higher2<Writer.µ, L, B>> map) {
    Writer<L, B> apply = map.andThen(Writer::narrowK).apply(value);
    return new Writer<>(monoid, monoid.combine(log, apply.log), apply.value);
  }

  public static <L, A> Writer<L, A> pure(Monoid<L> monoid, A value) {
    return new Writer<>(monoid, monoid.zero(), value);
  }

  public static <L, A> Writer<L, A> writer(Monoid<L> monoid, L log, A value) {
    return new Writer<>(monoid, log, value);
  }

  public static <T, A> Writer<Sequence<T>, A> listPure(A value) {
    return pure(Monoid.sequence(), value);
  }

  public static <T, A> Writer<Sequence<T>, A> listWriter(T log, A value) {
    return writer(Monoid.sequence(), listOf(log), value);
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
