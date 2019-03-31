/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Matcher3.invalid;
import static java.util.Objects.requireNonNull;

public class Pattern3<A, B, C, R> implements PartialFunction3<A, B, C, R> {

  private final Pattern1<Tuple3<A, B, C>, R> pattern;

  private Pattern3() {
    this(Pattern1.build());
  }

  private Pattern3(Pattern1<Tuple3<A, B, C>, R> pattern) {
    this.pattern = requireNonNull(pattern);
  }

  @Override
  public boolean isDefinedAt(A a, B b, C c) {
    return pattern.isDefinedAt(Tuple.of(a, b, c));
  }

  @Override
  public R apply(A a, B b, C c) {
    return pattern.apply(Tuple.of(a, b, c));
  }

  public static <A, B, C, R> Pattern3<A, B, C, R> build() {
    return new Pattern3<>();
  }

  public CaseBuilder3<Pattern3<A, B, C, R>, A, B, C, R> when(Matcher3<A, B, C> matcher) {
    return new CaseBuilder3<>(this::add).when(matcher);
  }

  public CaseBuilder3<Pattern3<A, B, C, R>, A, B, C, R> otherwise() {
    return new CaseBuilder3<>(this::add).when(Matcher3.otherwise());
  }

  private Pattern3<A, B, C, R> add(Matcher3<A, B, C> matcher, Function3<A, B, C, R> handler) {
    return new Pattern3<>(pattern.add(matcher.tupled(), handler.tupled()));
  }

  public final static class CaseBuilder3<B, T, V, U, R> {

    private final Function2<Matcher3<T, V, U>, Function3<T, V, U, R>, B> finisher;
    private final Matcher3<T, V, U> matcher;

    private CaseBuilder3(Function2<Matcher3<T, V, U>, Function3<T, V, U, R>, B> finisher) {
      this.finisher = requireNonNull(finisher);
      this.matcher = invalid();
    }

    private CaseBuilder3(Function2<Matcher3<T, V, U>, Function3<T, V, U, R>, B> finisher, Matcher3<T, V, U> matcher) {
      this.finisher = requireNonNull(finisher);
      this.matcher = requireNonNull(matcher);
    }

    public CaseBuilder3<B, T, V, U, R> when(Matcher3<T, V, U> matcher) {
      return new CaseBuilder3<>(finisher, matcher);
    }

    public B then(Function3<T, V, U, R> handler) {
      return finisher.apply(matcher, handler);
    }

    public B returns(R value) {
      return then((a, b, c) -> value);
    }
  }
}
