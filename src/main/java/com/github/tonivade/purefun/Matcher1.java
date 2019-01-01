/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.nonNull;

import java.util.Objects;
import java.util.stream.Stream;

@FunctionalInterface
public interface Matcher1<A> {

  boolean match(A target);

  default Matcher1<A> and(Matcher1<A> other) {
    return value -> match(value) && other.match(value);
  }

  default Matcher1<A> or(Matcher1<A> other) {
    return value -> match(value) || other.match(value);
  }

  default Matcher1<A> negate() {
    return value -> !match(value);
  }

  static <A> Matcher1<A> not(Matcher1<A> matcher) {
    return matcher.negate();
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
    Objects.requireNonNull(type);
    return value -> nonNull(value) && type.isAssignableFrom(value.getClass());
  }

  static <A> Matcher1<A> is(A other) {
    Objects.requireNonNull(other);
    return value -> Objects.equals(value, other);
  }

  @SafeVarargs
  static <A> Matcher1<A> isIn(A... values) {
    Objects.requireNonNull(values);
    return target -> Stream.of(values).anyMatch(value -> Objects.equals(target, value));
  }

  static <A> Matcher1<A> isNull() {
    return Objects::isNull;
  }

  static <A> Matcher1<A> isNotNull() {
    return Objects::nonNull;
  }

  @SafeVarargs
  static <A> Matcher1<A> allOf(Matcher1<A>... matchers) {
    Objects.requireNonNull(matchers);
    return target -> Stream.of(matchers).allMatch(matcher -> matcher.match(target));
  }

  @SafeVarargs
  static <A> Matcher1<A> anyOf(Matcher1<A>... matchers) {
    Objects.requireNonNull(matchers);
    return target -> Stream.of(matchers).anyMatch(matcher -> matcher.match(target));
  }

  static <A> Matcher1<A> otherwise() {
    return value -> true;
  }
}
