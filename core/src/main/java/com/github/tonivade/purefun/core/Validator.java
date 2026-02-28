/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Matcher1.isNotNull;
import static com.github.tonivade.purefun.core.Matcher1.not;
import static com.github.tonivade.purefun.core.Precondition.check;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Precondition.checkPositive;
import static com.github.tonivade.purefun.type.Validation.mapN;

import java.util.Locale;
import java.util.regex.Pattern;

import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.type.Validation.Result;

@FunctionalInterface
public interface Validator<E, T> {

  Validation<E, T> validate(T value);

  default <F> Validator<F, T> mapError(Function1<? super E, ? extends F> mapper) {
    checkNonNull(mapper);
    return value -> validate(value).mapError(mapper);
  }

  default <R> Validator<E, R> compose(Function1<? super R, ? extends T> getter) {
    checkNonNull(getter);
    return value -> validate(getter.apply(value)).map(Function1.cons(value));
  }

  default Validator<E, T> andThen(Validator<E, T> then) {
    checkNonNull(then);
    return value -> validate(value).flatMap(then::validate);
  }

  default Validator<Result<E>, T> combine(Validator<E, T> other) {
    checkNonNull(other);
    return combine(this, other);
  }

  static <E, T> Validator<E, T> from(Matcher1<? super T> matcher, Producer<? extends E> error) {
    checkNonNull(matcher);
    checkNonNull(error);
    return value -> matcher.match(value) ? Validation.valid(value) : Validation.invalid(error.get());
  }

  static <E, A, B> Validator<Result<E>, Tuple2<A, B>> product(Validator<E, A> v1,
                                                              Validator<E, B> v2) {
    return product(v1, v2, identity());
  }

  static <E, F, A, B> Validator<F, Tuple2<A, B>> product(Validator<E, A> v1,
                                                         Validator<E, B> v2,
                                                         Function1<Result<E>, F> reduce) {
    checkNonNull(v1);
    checkNonNull(v2);
    checkNonNull(reduce);
    return value -> mapN(
        v1.validate(value.get1()),
        v2.validate(value.get2()),
        Function2.cons(value))
        .mapError(reduce);
  }

  static <E, A, B, C> Validator<Result<E>, Tuple3<A, B, C>> product(Validator<E, A> v1,
                                                                    Validator<E, B> v2,
                                                                    Validator<E, C> v3) {
    return product(v1, v2, v3, identity());
  }

  static <E, F, A, B, C> Validator<F, Tuple3<A, B, C>> product(Validator<E, A> v1,
                                                               Validator<E, B> v2,
                                                               Validator<E, C> v3,
                                                               Function1<Result<E>, F> reduce) {
    checkNonNull(v1);
    checkNonNull(v2);
    checkNonNull(v3);
    checkNonNull(reduce);
    return value -> mapN(
        v1.validate(value.get1()),
        v2.validate(value.get2()),
        v3.validate(value.get3()),
        Function3.cons(value))
        .mapError(reduce);
  }

  static <E, A, B, C, D> Validator<Result<E>, Tuple4<A, B, C, D>> product(Validator<E, A> v1,
                                                                          Validator<E, B> v2,
                                                                          Validator<E, C> v3,
                                                                          Validator<E, D> v4) {
    return product(v1, v2, v3, v4, identity());
  }

  static <E, F, A, B, C, D> Validator<F, Tuple4<A, B, C, D>> product(Validator<E, A> v1,
                                                                     Validator<E, B> v2,
                                                                     Validator<E, C> v3,
                                                                     Validator<E, D> v4,
                                                                     Function1<Result<E>, F> reduce) {
    checkNonNull(v1);
    checkNonNull(v2);
    checkNonNull(v3);
    checkNonNull(v4);
    checkNonNull(reduce);
    return value -> mapN(
        v1.validate(value.get1()),
        v2.validate(value.get2()),
        v3.validate(value.get3()),
        v4.validate(value.get4()),
        Function4.cons(value))
        .mapError(reduce);
  }

  static <F, A, B, C, D, E> Validator<Result<F>, Tuple5<A, B, C, D, E>> product(Validator<F, A> v1,
                                                                                Validator<F, B> v2,
                                                                                Validator<F, C> v3,
                                                                                Validator<F, D> v4,
                                                                                Validator<F, E> v5) {
    return product(v1, v2, v3, v4, v5, identity());
  }

  static <F, G, A, B, C, D, E> Validator<G, Tuple5<A, B, C, D, E>> product(Validator<F, A> v1,
                                                                           Validator<F, B> v2,
                                                                           Validator<F, C> v3,
                                                                           Validator<F, D> v4,
                                                                           Validator<F, E> v5,
                                                                           Function1<Result<F>, G> reduce) {
    checkNonNull(v1);
    checkNonNull(v2);
    checkNonNull(v3);
    checkNonNull(v4);
    checkNonNull(v5);
    checkNonNull(reduce);
    return value -> mapN(
        v1.validate(value.get1()),
        v2.validate(value.get2()),
        v3.validate(value.get3()),
        v4.validate(value.get4()),
        v5.validate(value.get5()),
        Function5.cons(value))
        .mapError(reduce);
  }

  static <E, T> Validator<Result<E>, T> combine(Validator<E, T> v1,
                                                Validator<E, T> v2) {
    return combine(v1, v2, identity());
  }

  static <E, F, T> Validator<F, T> combine(Validator<E, T> v1,
                                           Validator<E, T> v2,
                                           Function1<Result<E>, F> reduce) {
    checkNonNull(v1);
    checkNonNull(v2);
    checkNonNull(reduce);
    return value -> mapN(
        v1.validate(value),
        v2.validate(value),
        Function2.cons(value))
        .mapError(reduce);
  }

  static <E, T> Validator<Result<E>, T> combine(Validator<E, T> v1,
                                                Validator<E, T> v2,
                                                Validator<E, T> v3) {
    return combine(v1, v2, v3, identity());
  }

  static <E, F, T> Validator<F, T> combine(Validator<E, T> v1,
                                           Validator<E, T> v2,
                                           Validator<E, T> v3,
                                           Function1<Result<E>, F> reduce) {
    return combine(combine(v1, v2), v3.mapError(Result::of), Result.<E>flatten().andThen(reduce));
  }

  static <E, T> Validator<Result<E>, T> combine(Validator<E, T> v1,
                                                Validator<E, T> v2,
                                                Validator<E, T> v3,
                                                Validator<E, T> v4) {
    return combine(v1, v2, v3, v4, identity());
  }

  static <E, F, T> Validator<F, T> combine(Validator<E, T> v1,
                                           Validator<E, T> v2,
                                           Validator<E, T> v3,
                                           Validator<E, T> v4,
                                           Function1<Result<E>, F> reduce) {
    return combine(combine(v1, v2, v3), v4.mapError(Result::of), Result.<E>flatten().andThen(reduce));
  }

  static <E, T> Validator<Result<E>, T> combine(Validator<E, T> v1,
                                                Validator<E, T> v2,
                                                Validator<E, T> v3,
                                                Validator<E, T> v4,
                                                Validator<E, T> v5) {
    return combine(v1, v2, v3, v4, v5, identity());
  }

  static <E, F, T> Validator<F, T> combine(Validator<E, T> v1,
                                           Validator<E, T> v2,
                                           Validator<E, T> v3,
                                           Validator<E, T> v4,
                                           Validator<E, T> v5,
                                           Function1<Result<E>, F> reduce) {
    return combine(combine(v1, v2, v3, v4), v5.mapError(Result::of), Result.<E>flatten().andThen(reduce));
  }

  static <E, T> Validator<E, T> valid() {
    return Validation::valid;
  }

  static <E, T> Validator<E, T> invalid(E error) {
    return value -> Validation.invalid(error);
  }

  static <T> Validator<String, T> nonNull() {
    return nonNull(() -> "require non null");
  }

  static <T> Validator<String, T> nonNull(Producer<String> message) {
    return Validator.from(isNotNull(), message);
  }

  static <T> Validator<String, T> nonNullAnd(Validator<String, T> then) {
    return nonNullAnd(() -> "require non null", then);
  }

  static <T> Validator<String, T> nonNullAnd(Producer<String> message, Validator<String, T> then) {
    return Validator.<T>nonNull(message).andThen(then);
  }

  static <T> Validator<String, T> equalsTo(T expected) {
    return equalsTo(expected, () -> "require equals to " + expected);
  }

  static <T> Validator<String, T> equalsTo(T expected, Producer<String> message) {
    checkNonNull(expected, "expected should not be null");
    return from(Matcher1.is(expected), message);
  }

  static <T> Validator<String, T> notEqualsTo(T expected) {
    return notEqualsTo(expected, () -> "require not equals to " + expected);
  }

  static <T> Validator<String, T> notEqualsTo(T expected, Producer<String> message) {
    checkNonNull(expected, "expected should not be null");
    return from(Matcher1.is(expected).negate(), message);
  }

  static <T> Validator<String, T> instanceOf(Class<?> clazz) {
    return instanceOf(clazz, () -> "require instance of " + clazz);
  }

  static <T> Validator<String, T> instanceOf(Class<?> clazz, Producer<String> message) {
    checkNonNull(clazz, "expected should not be null");
    return from(Matcher1.instanceOf(clazz), message);
  }

  static Validator<String, String> nonEmpty() {
    return nonEmpty(() -> "require non empty string");
  }

  static Validator<String, String> nonEmpty(Producer<String> message) {
    return from(not(String::isEmpty), message);
  }

  static Validator<String, String> upper() {
    return upper(() -> "require uppercase string");
  }

  static Validator<String, String> upper(Producer<String> message) {
    return from(value -> value.toUpperCase(Locale.ROOT).equals(value), message);
  }

  static Validator<String, String> lower() {
    return lower(() -> "require lowercase string");
  }

  static Validator<String, String> lower(Producer<String> message) {
    return from(value -> value.toLowerCase(Locale.ROOT).equals(value), message);
  }

  static Validator<String, String> startsWith(String prefix) {
    return startsWith(prefix, () -> "require start with: " + prefix);
  }

  static Validator<String, String> startsWith(String prefix, Producer<String> message) {
    checkNonNull(prefix, "prefix should not be null");
    return from(value -> value.startsWith(prefix), message);
  }

  static Validator<String, String> contains(String substring) {
    return contains(substring, () -> "require contain string: " + substring);
  }

  static Validator<String, String> contains(String substring, Producer<String> message) {
    checkNonNull(substring, "substring should not be null");
    return from(value -> value.contains(substring), message);
  }

  static Validator<String, String> endsWith(String suffix) {
    return endsWith(suffix, () -> "require end with: " + suffix);
  }

  static Validator<String, String> endsWith(String suffix, Producer<String> message) {
    checkNonNull(suffix, "suffix should not be null");
    return from(value -> value.endsWith(suffix), message);
  }

  static Validator<String, String> match(String regex) {
    return match(regex, () -> "should match expresion: " + regex);
  }

  static Validator<String, String> match(String regex, Producer<String> message) {
    checkNonNull(regex, "regex should not be null");
    return from(value -> Pattern.matches(regex, value), message);
  }

  static Validator<String, String> minLength(int length) {
    return minLength(length, () -> "require min length: " + length);
  }

  static Validator<String, String> minLength(int length, Producer<String> message) {
    checkPositive(length, "length should be a positive value");
    return greaterThanOrEqual(length, message).compose(String::length);
  }

  static Validator<String, String> maxLength(int length) {
    return maxLength(length, () -> "require max length: " + length);
  }

  static Validator<String, String> maxLength(int length, Producer<String> message) {
    checkPositive(length, "length should be a positive value");
    return lowerThan(length, message).compose(String::length);
  }

  static <T extends Comparable<T>> Validator<String, T> nonEquals(T value) {
    return nonEquals(value, () -> "require non equals to " + value);
  }

  static <T extends Comparable<T>> Validator<String, T> nonEquals(T value, Producer<String> message) {
    return from(input -> input.compareTo(value) != 0, message);
  }

  static Validator<String, Integer> positive() {
    return greaterThan(0);
  }

  static Validator<String, Integer> positive(Producer<String> message) {
    return greaterThan(0, message);
  }

  static Validator<String, Integer> negative() {
    return lowerThan(0);
  }

  static Validator<String, Integer> negative(Producer<String> message) {
    return lowerThan(0, message);
  }

  static <T extends Comparable<T>> Validator<String, T> greaterThan(T min) {
    return greaterThan(min, () -> "require greater than: " + min);
  }

  static <T extends Comparable<T>> Validator<String, T> greaterThan(T min, Producer<String> message) {
    return from(value -> min.compareTo(value) < 0, message);
  }

  static <T extends Comparable<T>> Validator<String, T> greaterThanOrEqual(T min) {
    return greaterThanOrEqual(min, () -> "require greater than or equal to: " + min);
  }

  static <T extends Comparable<T>> Validator<String, T> greaterThanOrEqual(T min, Producer<String> message) {
    return from(value -> min.compareTo(value) <= 0, message);
  }

  static <T extends Comparable<T>> Validator<String, T> lowerThan(T max) {
    return lowerThan(max, () -> "require lower than: " + max);
  }

  static <T extends Comparable<T>> Validator<String, T> lowerThan(T max, Producer<String> message) {
    return from(value -> value.compareTo(max) < 0, message);
  }

  static <T extends Comparable<T>> Validator<String, T> lowerThanOrEqual(T max) {
    return lowerThanOrEqual(max, () -> "require lower than: " + max);
  }

  static <T extends Comparable<T>> Validator<String, T> lowerThanOrEqual(T max, Producer<String> message) {
    return from(value -> value.compareTo(max) <= 0, message);
  }

  static Validator<String, String> length(int min, int max) {
    return length(min, max, Producer.cons(""));
  }

  static Validator<String, String> length(int min, int max, Producer<String> message) {
    check(() -> min < max, min + " should not be greater than " + max);
    return Validator.<String>nonNull()
        .andThen(combine(minLength(min), maxLength(max), join(message)));
  }

  static Validator<String, Integer> range(int start, int end) {
    return range(start, end, Producer.cons(""));
  }

  static Validator<String, Integer> range(int start, int end, Producer<String> message) {
    check(() -> start < end, "start should not be greater than end");
    return Validator.<Integer>nonNull()
        .andThen(combine(greaterThanOrEqual(start), lowerThan(end), join(message)));
  }

  static <E> Function1<Result<E>, String> join() {
    return result -> result.join(",");
  }

  static <E> Function1<Result<E>, String> join(String separator) {
    return result -> result.join(separator);
  }

  static <E> Function1<Result<E>, String> join(Producer<String> message) {
    return result -> result.join(",", message);
  }

  static <E> Function1<Result<E>, String> join(String separator, Producer<String> message) {
    return result -> result.join(separator, message);
  }
}
