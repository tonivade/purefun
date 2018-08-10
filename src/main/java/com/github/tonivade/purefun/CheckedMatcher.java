/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface CheckedMatcher<T> {

  boolean match(T target);
  
  default CheckedMatcher<T> and(CheckedMatcher<T> other) {
    return request -> match(request) && other.match(request);
  }
  
  default CheckedMatcher<T> or(CheckedMatcher<T> other) {
    return request -> match(request) || other.match(request);
  }
  
  default CheckedMatcher<T> negate() {
    return request -> !match(request);
  }
  
  static <T> CheckedMatcher<T> not(CheckedMatcher<T> matcher) {
    return matcher.negate();
  }
}
