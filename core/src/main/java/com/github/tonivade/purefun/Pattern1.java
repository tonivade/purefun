/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.fail;
import static com.github.tonivade.purefun.Matcher1.never;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

public final class Pattern1<A, R> implements PartialFunction1<A, R> {

  private final PartialFunction1<A, R> function;

  private Pattern1(PartialFunction1<A, R> function) {
    this.function = checkNonNull(function);
  }

  public static <A, R> Pattern1<A, R> build() {
    return new Pattern1<>(PartialFunction1.of(never(), fail(UnsupportedOperationException::new)));
  }

  public ThenStep<Pattern1<A, R>, A, R> when(Matcher1<A> matcher) {
    return handler -> add(matcher, handler);
  }

  @SuppressWarnings("unchecked")
  public <T> ThenStep<Pattern1<A, R>, T, R> when(Class<T> type) {
    return handler -> add(Matcher1.instanceOf(type), value -> handler.apply((T) value));
  }

  public ThenStep<Pattern1<A, R>, A, R> otherwise() {
    return handler -> add(Matcher1.otherwise(), handler);
  }

  @Override
  public R apply(A value) {
    return function.apply(value);
  }

  @Override
  public boolean isDefinedAt(A value) {
    return function.isDefinedAt(value);
  }

  protected Pattern1<A, R> add(Matcher1<A> matcher, Function1<A, R> handler) {
    return new Pattern1<>(function.orElse(PartialFunction1.of(matcher, handler)));
  }
  
  @FunctionalInterface
  public interface ThenStep<P, T, R> {

    P then(Function1<T, R> handler);
    
    default P returns(R value) {
      return then(cons(value));
    }
  }
}
