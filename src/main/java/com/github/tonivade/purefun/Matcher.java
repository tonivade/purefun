/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.nonNull;

import java.util.Objects;

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
  
  static <T> Matcher<T> instanceOf(Class<? extends T> type) {
    Objects.requireNonNull(type);
    return value -> nonNull(value) && type.isAssignableFrom(value.getClass());
  }
  
  static <T> Matcher<T> otherwise() {
    return value -> true;
  }
}
