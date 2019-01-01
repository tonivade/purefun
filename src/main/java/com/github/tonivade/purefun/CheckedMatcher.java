/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface CheckedMatcher<A> {

  boolean match(A target) throws Throwable;

  default CheckedMatcher<A> and(CheckedMatcher<A> other) {
    return request -> match(request) && other.match(request);
  }

  default CheckedMatcher<A> or(CheckedMatcher<A> other) {
    return request -> match(request) || other.match(request);
  }

  default CheckedMatcher<A> negate() {
    return request -> !match(request);
  }

  static <A> CheckedMatcher<A> not(CheckedMatcher<A> matcher) {
    return matcher.negate();
  }
}
