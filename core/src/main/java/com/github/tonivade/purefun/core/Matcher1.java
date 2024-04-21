/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static java.util.Objects.nonNull;

import java.util.Objects;
import java.util.stream.Stream;

@FunctionalInterface
public interface Matcher1<A> extends Recoverable {

  default boolean match(A target) {
    try {
      return run(target);
    } catch (Throwable t) {
      return sneakyThrow(t);
    }
  }

  boolean run(A target) throws Throwable;
  
  default Function1<A, Boolean> asFunction() {
    return this::match;
  }

  default Matcher1<A> and(Matcher1<? super A> other) {
    return value -> match(value) && other.match(value);
  }

  default Matcher1<A> or(Matcher1<? super A> other) {
    return value -> match(value) || other.match(value);
  }

  default Matcher1<A> negate() {
    return value -> !match(value);
  }

  @SuppressWarnings("unchecked")
  static <A> Matcher1<A> not(Matcher1<? super A> matcher) {
    return (Matcher1<A>) matcher.negate();
  }

  static <A> Matcher1<A> never() {
    return value -> false;
  }

  static <A> Matcher1<A> always() {
    return value -> true;
  }

  static <A> Matcher1<A> invalid() {
    return value -> { throw new IllegalStateException(); };
  }

  // XXX: when I change Class<?> for Class<? extends T>
  // javac complains about this, it cannot infer type parameters but inside eclipse works fine
  static <A> Matcher1<A> instanceOf(Class<?> type) {
    checkNonNull(type);
    return value -> nonNull(value) && type.isAssignableFrom(value.getClass());
  }

  static <A> Matcher1<A> is(A other) {
    checkNonNull(other);
    return value -> Objects.equals(value, other);
  }

  @SafeVarargs
  static <A> Matcher1<A> isIn(A... values) {
    checkNonNull(values);
    return target -> Stream.of(values).anyMatch(value -> Objects.equals(target, value));
  }

  static <A> Matcher1<A> isNull() {
    return Objects::isNull;
  }

  static <A> Matcher1<A> isNotNull() {
    return Objects::nonNull;
  }

  @SafeVarargs
  static <A> Matcher1<A> allOf(Matcher1<? super A>... matchers) {
    checkNonNull(matchers);
    return target -> Stream.of(matchers).allMatch(matcher -> matcher.match(target));
  }

  @SafeVarargs
  static <A> Matcher1<A> anyOf(Matcher1<? super A>... matchers) {
    checkNonNull(matchers);
    return target -> Stream.of(matchers).anyMatch(matcher -> matcher.match(target));
  }

  static <A> Matcher1<A> otherwise() {
    return value -> true;
  }
}
