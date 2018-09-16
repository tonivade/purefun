/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.type.Option;

public final class Pattern<T, R> implements Function1<T, R> {

  private final ImmutableList<Case<T, R>> cases;

  private Pattern() {
    this(ImmutableList.empty());
  }

  private Pattern(ImmutableList<Case<T, R>> cases) {
    this.cases = requireNonNull(cases);
  }

  public static <T, R> Pattern<T, R> build() {
    return new Pattern<>();
  }

  public CaseBuilder<Pattern<T, R>, T, R> when(Matcher<T> matcher) {
    return new CaseBuilder<>(this::add).when(matcher);
  }

  public CaseBuilder<Pattern<T, R>, T, R> otherwise() {
    return new CaseBuilder<>(this::add).when(Matcher.otherwise());
  }

  @Override
  public R apply(T target) {
    return findCase(target)
        .map(case_ -> case_.apply(target))
        .orElseThrow(IllegalStateException::new);
  }

  private Pattern<T, R> add(Matcher<T> matcher, Function1<T, R> handler) {
    return new Pattern<>(cases.append(new Case<>(matcher, handler)));
  }

  private Option<Case<T, R>> findCase(T target) {
    return cases.filter(case_ -> case_.match(target)).head();
  }

  public static final class Case<T, R> {

    private final Matcher<T> matcher;
    private final Function1<T, R> handler;

    private Case(Matcher<T> matcher, Function1<T, R> handler) {
      this.matcher = requireNonNull(matcher);
      this.handler = requireNonNull(handler);
    }

    public boolean match(T value) {
      return matcher.match(value);
    }

    public R apply(T value) {
      return handler.apply(value);
    }
  }

  public static final class CaseBuilder<B, T, R> {

    private final Function2<Matcher<T>, Function1<T, R>, B> finisher;
    private final Matcher<T> matcher;

    private CaseBuilder(Function2<Matcher<T>, Function1<T, R>, B> finisher) {
      this(finisher, null);
    }

    private CaseBuilder(Function2<Matcher<T>, Function1<T, R>, B> finisher, Matcher<T> matcher) {
      this.finisher = requireNonNull(finisher);
      this.matcher = matcher;
    }

    public CaseBuilder<B, T, R> when(Matcher<T> matcher) {
      return new CaseBuilder<>(finisher, matcher);
    }

    public B then(Function1<T, R> handler) {
      return finisher.apply(requireNonNull(matcher), requireNonNull(handler));
    }

    // XXX: I have to rename this method because eclipse complains, it says that there are ambiguous.
    // javac compiler works fine.
    // related bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=522380
    public B returns(R value) {
      return then(target -> value);
    }
  }
}
