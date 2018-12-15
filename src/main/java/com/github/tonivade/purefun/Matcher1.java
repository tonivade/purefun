/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.nonNull;

import java.util.Objects;
import java.util.stream.Stream;

@FunctionalInterface
public interface Matcher1<T> {

  boolean match(T target);

  default Matcher1<T> and(Matcher1<T> other) {
    return value -> match(value) && other.match(value);
  }

  default Matcher1<T> or(Matcher1<T> other) {
    return value -> match(value) || other.match(value);
  }

  default Matcher1<T> negate() {
    return value -> !match(value);
  }

  static <T> Matcher1<T> not(Matcher1<T> matcher) {
    return matcher.negate();
  }

  static <T> Matcher1<T> invalid() {
    return value -> { throw new IllegalStateException(); };
  }

  // XXX: when I change Class<?> for Class<? extends T>
  // javac complains about this, it cannot infer type parameters but inside eclipse works fine
  static <T> Matcher1<T> instanceOf(Class<?> type) {
    Objects.requireNonNull(type);
    return value -> nonNull(value) && type.isAssignableFrom(value.getClass());
  }

  static <T> Matcher1<T> is(T other) {
    Objects.requireNonNull(other);
    return value -> Objects.equals(value, other);
  }

  @SafeVarargs
  static <T> Matcher1<T> isIn(T... values) {
    Objects.requireNonNull(values);
    return target -> Stream.of(values).anyMatch(value -> Objects.equals(target, value));
  }

  static <T> Matcher1<T> isNull() {
    return Objects::isNull;
  }

  static <T> Matcher1<T> isNotNull() {
    return Objects::nonNull;
  }

  @SafeVarargs
  static <T> Matcher1<T> allOf(Matcher1<T>... matchers) {
    Objects.requireNonNull(matchers);
    return target -> Stream.of(matchers).allMatch(matcher -> matcher.match(target));
  }

  @SafeVarargs
  static <T> Matcher1<T> anyOf(Matcher1<T>... matchers) {
    Objects.requireNonNull(matchers);
    return target -> Stream.of(matchers).anyMatch(matcher -> matcher.match(target));
  }

  static <T> Matcher1<T> otherwise() {
    return value -> true;
  }
}
