/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.nonNull;

import java.util.Objects;
import java.util.stream.Stream;

@FunctionalInterface
public interface Matcher<T> {

  boolean match(T target);

  default Matcher<T> and(Matcher<T> other) {
    return value -> match(value) && other.match(value);
  }

  default Matcher<T> or(Matcher<T> other) {
    return value -> match(value) || other.match(value);
  }

  default Matcher<T> negate() {
    return value -> !match(value);
  }

  static <T> Matcher<T> not(Matcher<T> matcher) {
    return matcher.negate();
  }

  // XXX: when I change Class<?> for Class<? extends T>
  // javac complains about this, it cannot infer type parameters but inside eclipse works fine
  static <T> Matcher<T> instanceOf(Class<?> type) {
    Objects.requireNonNull(type);
    return value -> nonNull(value) && type.isAssignableFrom(value.getClass());
  }

  static <T> Matcher<T> is(T other) {
    Objects.requireNonNull(other);
    return value -> Objects.equals(value, other);
  }

  @SafeVarargs
  static <T> Matcher<T> isIn(T... values) {
    Objects.requireNonNull(values);
    return target -> Stream.of(values).anyMatch(value -> Objects.equals(target, value));
  }

  static <T> Matcher<T> isNull() {
    return Objects::isNull;
  }

  static <T> Matcher<T> isNotNull() {
    return Objects::nonNull;
  }

  @SafeVarargs
  static <T> Matcher<T> allOf(Matcher<T>... matchers) {
    Objects.requireNonNull(matchers);
    return target -> Stream.of(matchers).allMatch(matcher -> matcher.match(target));
  }

  @SafeVarargs
  static <T> Matcher<T> anyOf(Matcher<T>... matchers) {
    Objects.requireNonNull(matchers);
    return target -> Stream.of(matchers).anyMatch(matcher -> matcher.match(target));
  }

  static <T> Matcher<T> otherwise() {
    return value -> true;
  }
}
