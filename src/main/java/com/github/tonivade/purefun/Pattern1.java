/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.fail;
import static com.github.tonivade.purefun.Matcher1.invalid;
import static com.github.tonivade.purefun.Matcher1.never;
import static java.util.Objects.requireNonNull;

public final class Pattern1<A, R> implements PartialFunction1<A, R> {

  private final PartialFunction1<A, R> function;

  private Pattern1() {
    this(PartialFunction1.of(fail(), never()));
  }

  private Pattern1(PartialFunction1<A, R> function) {
    this.function = requireNonNull(function);
  }

  public static <A, R> Pattern1<A, R> build() {
    return new Pattern1<>();
  }

  public CaseBuilder1<Pattern1<A, R>, A, R> when(Matcher1<A> matcher) {
    return new CaseBuilder1<>(this::add).when(matcher);
  }

  public CaseBuilder1<Pattern1<A, R>, A, R> otherwise() {
    return new CaseBuilder1<>(this::add).when(Matcher1.otherwise());
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
    return new Pattern1<>(function.orElse(PartialFunction1.of(handler, matcher)));
  }

  public static final class CaseBuilder1<B, T, R> {

    private final Function2<Matcher1<T>, Function1<T, R>, B> finisher;
    private final Matcher1<T> matcher;

    private CaseBuilder1(Function2<Matcher1<T>, Function1<T, R>, B> finisher) {
      this.finisher = requireNonNull(finisher);
      this.matcher = invalid();
    }

    private CaseBuilder1(Function2<Matcher1<T>, Function1<T, R>, B> finisher, Matcher1<T> matcher) {
      this.finisher = requireNonNull(finisher);
      this.matcher = requireNonNull(matcher);
    }

    public CaseBuilder1<B, T, R> when(Matcher1<T> matcher) {
      return new CaseBuilder1<>(finisher, matcher);
    }

    public B then(Function1<T, R> handler) {
      return finisher.apply(matcher, handler);
    }

    // XXX: I have to rename this method because eclipse complains, it says that there are ambiguous.
    // javac compiler works fine.
    // related bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=522380
    public B returns(R value) {
      return then(cons(value));
    }
  }
}
