/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Matcher1.invalid;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.type.Option;

public final class Pattern1<A, R> implements Function1<A, R> {

  private final ImmutableList<Case<A, R>> cases;

  private Pattern1() {
    this(ImmutableList.empty());
  }

  private Pattern1(ImmutableList<Case<A, R>> cases) {
    this.cases = requireNonNull(cases);
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
  public R apply(A target) {
    return findCase(target)
        .map(case_ -> case_.apply(target))
        .getOrElseThrow(IllegalStateException::new);
  }

  protected Pattern1<A, R> add(Matcher1<A> matcher, Function1<A, R> handler) {
    return new Pattern1<>(cases.append(new Case<>(matcher, handler)));
  }

  private Option<Case<A, R>> findCase(A target) {
    return cases.filter(case_ -> case_.match(target)).head();
  }

  public static final class Case<A, R> {

    private final Matcher1<A> matcher;
    private final Function1<A, R> handler;

    Case(Matcher1<A> matcher, Function1<A, R> handler) {
      this.matcher = requireNonNull(matcher);
      this.handler = requireNonNull(handler);
    }

    public boolean match(A value) {
      return matcher.match(value);
    }

    public R apply(A value) {
      return handler.apply(value);
    }
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
      return then(target -> value);
    }
  }
}
