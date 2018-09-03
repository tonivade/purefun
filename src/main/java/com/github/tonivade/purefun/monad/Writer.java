/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.monad.WriterKind.narrowK;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Monad2;
import com.github.tonivade.purefun.Monoid;

public final class Writer<L, A> implements Monad2<WriterKind.µ, L, A> {

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
  public <B> Writer<L, B> flatMap(Function1<A, ? extends Monad2<WriterKind.µ, L, B>> map) {
    Writer<L, B> apply = narrowK(map.apply(value));
    return new Writer<>(monoid, monoid.combine(log, apply.log), apply.value);
  }

  public static <L, A> Writer<L, A> pure(Monoid<L> monoid, A value) {
    return new Writer<>(monoid, monoid.zero(), value);
  }

  public static <L, A> Writer<L, A> writer(Monoid<L> monoid, L log, A value) {
    return new Writer<>(monoid, log, value);
  }
}
