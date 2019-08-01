/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface CheckedMatcher<A> extends Recoverable {

  boolean match(A target) throws Throwable;

  default Matcher1<A> unchecked() {
    return recover(this::sneakyThrow);
  }

  default Matcher1<A> recover(Function1<Throwable, Boolean> mapper) {
    return target -> {
      try {
        return match(target);
      } catch (Throwable e) {
        return mapper.apply(e);
      }
    };
  }

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
