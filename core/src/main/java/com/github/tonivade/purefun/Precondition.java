/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface Precondition {

  boolean apply();

  default Precondition negate() {
    return () -> !apply();
  }

  default Precondition and(Precondition other) {
    return () -> apply() && other.apply();
  }

  static Precondition nonNull(Object value) {
    return () -> value != null;
  }

  static Precondition empty(String value) {
    return () -> value.isEmpty();
  }

  static Precondition nonEmpty(String value) {
    return empty(value).negate();
  }

  static Precondition positive(int value) {
    return greaterThan(value, 0);
  }

  static Precondition negative(int value) {
    return lowerThan(value, 0);
  }

  static Precondition greaterThan(int value, int min) {
    return () -> value > min;
  }

  static Precondition greaterThanOrEquals(int value, int min) {
    return () -> value >= min;
  }

  static Precondition lowerThan(int value, int max) {
    return () -> value < max;
  }

  static Precondition lowerThanOrEquals(int value, int max) {
    return () -> value <= max;
  }

  static Precondition range(int value, int min, int max) {
    return greaterThan(max, min)
        .and(greaterThanOrEquals(value, min))
        .and(lowerThan(value, max));
  }

  static <T> T checkNonNull(T value) {
    return checkNonNull(value, "non null value required");
  }

  static <T> T checkNonNull(T value, String message) {
    check(nonNull(value), () -> message);
    return value;
  }

  static String checkNonEmpty(String value) {
    return checkNonEmpty(value, "non empty string required");
  }

  static String checkNonEmpty(String value, String message) {
    check(nonNull(value).and(nonEmpty(value)), () -> message);
    return value;
  }

  static int checkPositive(int value) {
    return checkPositive(value, "positive value required");
  }

  static int checkPositive(int value, String message) {
    check(positive(value), () -> message);
    return value;
  }

  static int checkNegative(int value) {
    return checkNegative(value, "negative value required");
  }

  static int checkNegative(int value, String message) {
    check(negative(value), () -> message);
    return value;
  }

  static int checkRange(int value, int min, int max) {
    return checkRange(value, min, max, "value not in range: " + min + "-" + max);
  }

  static int checkRange(int value, int min, int max, String message) {
    check(range(value, min, max), () -> message);
    return value;
  }

  static void check(Precondition precondition) {
    require(precondition, IllegalArgumentException::new);
  }

  static void check(Precondition precondition, String message) {
    check(precondition, () -> message);
  }

  static void check(Precondition precondition, Producer<String> message) {
    require(precondition, message.andThen(IllegalArgumentException::new));
  }

  static <X extends RuntimeException> void require(Precondition precondition, Producer<X> exception) {
    if (!precondition.apply()) {
      throw exception.get();
    }
  }
}
