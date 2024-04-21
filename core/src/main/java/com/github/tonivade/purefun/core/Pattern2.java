/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Function2.cons;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

public class Pattern2<A, B, R> implements PartialFunction2<A, B, R> {

  private final Pattern1<Tuple2<A, B>, R> pattern;

  private Pattern2() {
    this(Pattern1.build());
  }

  private Pattern2(Pattern1<Tuple2<A, B>, R> pattern) {
    this.pattern = checkNonNull(pattern);
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

  public ThenStep<Pattern2<A, B, R>, A, B, R> when(Matcher2<A, B> matcher) {
    return handler -> add(matcher, handler);
  }

  public ThenStep<Pattern2<A, B, R>, A, B, R> otherwise() {
    return handler -> add(Matcher2.otherwise(), handler);
  }

  private Pattern2<A, B, R> add(Matcher2<A, B> matcher, Function2<A, B, R> handler) {
    return new Pattern2<>(pattern.add(matcher.tupled(), handler.tupled()));
  }
  
  @FunctionalInterface
  public interface ThenStep<P, A, B, R> {
    
    P then(Function2<A, B, R> handler);
    
    default P returns(R value) {
      return then(cons(value));
    }
  }
}
