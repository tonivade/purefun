/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

@FunctionalInterface
public interface Matcher<T> {

  boolean match(T target);
  
  default Matcher<T> and(Matcher<T> other) {
    return request -> match(request) && other.match(request);
  }
  
  default Matcher<T> or(Matcher<T> other) {
    return request -> match(request) || other.match(request);
  }
  
  default Matcher<T> negate() {
    return request -> !match(request);
  }
  
  static <T> Matcher<T> not(Matcher<T> matcher) {
    return matcher.negate();
  }
}
