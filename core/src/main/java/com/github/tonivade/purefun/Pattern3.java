/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Function3.cons;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

public class Pattern3<A, B, C, R> implements PartialFunction3<A, B, C, R> {

  private final Pattern1<Tuple3<A, B, C>, R> pattern;

  private Pattern3() {
    this(Pattern1.build());
  }

  private Pattern3(Pattern1<Tuple3<A, B, C>, R> pattern) {
    this.pattern = checkNonNull(pattern);
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

  public ThenStep<Pattern3<A, B, C, R>, A, B, C, R> when(Matcher3<A, B, C> matcher) {
    return handler -> add(matcher, handler);
  }

  public ThenStep<Pattern3<A, B, C, R>, A, B, C, R> otherwise() {
    return handler -> add(Matcher3.otherwise(), handler);
  }

  private Pattern3<A, B, C, R> add(Matcher3<A, B, C> matcher, Function3<A, B, C, R> handler) {
    return new Pattern3<>(pattern.add(matcher.tupled(), handler.tupled()));
  }
  
  @FunctionalInterface
  public interface ThenStep<P, A, B, C, R> {
    
    P then(Function3<A, B, C, R> handler);
    
    default P returns(R value) {
      return then(cons(value));
    }
  }
}
