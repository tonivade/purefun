/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Matcher2.invalid;
import static java.util.Objects.requireNonNull;

public class Pattern2<A, B, R> implements PartialFunction2<A, B, R> {

  private final Pattern1<Tuple2<A, B>, R> pattern;

  private Pattern2() {
    this(Pattern1.build());
  }

  private Pattern2(Pattern1<Tuple2<A, B>, R> pattern) {
    this.pattern = requireNonNull(pattern);
  }

  @Override
  public boolean isDefinedAt(A a, B b) {
    return pattern.isDefinedAt(Tuple.of(a, b));
  }

  @Override
  public R apply(A a, B b) {
    return pattern.apply(Tuple.of(a, b));
  }

  public static <A, B, R> Pattern2<A, B, R> build() {
    return new Pattern2<>();
  }

  public CaseBuilder2<Pattern2<A, B, R>, A, B, R> when(Matcher2<A, B> matcher) {
    return new CaseBuilder2<>(this::add).when(matcher);
  }

  public CaseBuilder2<Pattern2<A, B, R>, A, B, R> otherwise() {
    return new CaseBuilder2<>(this::add).when(Matcher2.otherwise());
  }

  private Pattern2<A, B, R> add(Matcher2<A, B> matcher, Function2<A, B, R> handler) {
    return new Pattern2<>(pattern.add(matcher.tupled(), handler.tupled()));
  }

  public final static class CaseBuilder2<B, T, V, R> {

    private final Function2<Matcher2<T, V>, Function2<T, V, R>, B> finisher;
    private final Matcher2<T, V> matcher;

    private CaseBuilder2(Function2<Matcher2<T, V>, Function2<T, V, R>, B> finisher) {
      this.finisher = requireNonNull(finisher);
      this.matcher = invalid();
    }

    private CaseBuilder2(Function2<Matcher2<T, V>, Function2<T, V, R>, B> finisher, Matcher2<T, V> matcher) {
      this.finisher = requireNonNull(finisher);
      this.matcher = requireNonNull(matcher);
    }

    public CaseBuilder2<B, T, V, R> when(Matcher2<T, V> matcher) {
      return new CaseBuilder2<>(finisher, matcher);
    }

    public B then(Function2<T, V, R> handler) {
      return finisher.apply(matcher, handler);
    }

    public B returns(R value) {
      return then((a, b) -> value);
    }
  }
}
