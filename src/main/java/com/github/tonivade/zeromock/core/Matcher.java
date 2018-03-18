/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

@FunctionalInterface
public interface Matcher {

  boolean match(HttpRequest request);
  
  default Matcher and(Matcher other) {
    return request -> match(request) && other.match(request);
  }
  
  default Matcher or(Matcher other) {
    return request -> match(request) || other.match(request);
  }
  
  default Matcher negate() {
    return request -> !match(request);
  }
  
  static Matcher not(Matcher matcher) {
    return matcher.negate();
  }
}
