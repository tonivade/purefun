/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Tuple3;
import com.github.tonivade.purefun.Tuple4;
import com.github.tonivade.purefun.Tuple5;
import com.github.tonivade.purefun.data.Sequence;

import java.util.regex.Pattern;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Matcher1.isNotNull;
import static com.github.tonivade.purefun.Matcher1.not;
import static com.github.tonivade.purefun.type.Validation.map2;
import static com.github.tonivade.purefun.type.Validation.map3;
import static com.github.tonivade.purefun.type.Validation.map4;
import static com.github.tonivade.purefun.type.Validation.map5;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface Validator<E, T> {

  Validation<E, T> validate(T value);

  default <F> Validator<F, T> mapError(Function1<E, F> mapper) {
    requireNonNull(mapper);
    return value -> validate(value).mapError(mapper);
  }

  default <R> Validator<E, R> compose(Function1<R, T> getter) {
    requireNonNull(getter);
    return value -> validate(getter.apply(value)).map(Function1.cons(value));
  }

  default Validator<E, T> orElse(Validator<E, T> then) {
    requireNonNull(then);
    return value -> validate(value).flatMap(then::validate);
  }

  static <E, T> Validator<E, T> from(Matcher1<T> matcher, Producer<E> error) {
    requireNonNull(matcher);
    requireNonNull(error);
    return value -> matcher.match(value) ? Validation.valid(value) : Validation.invalid(error.get());
  }

  static <E, A, B> Validator<Sequence<E>, Tuple2<A, B>> product(Validator<E, A> v1,
                                                                Validator<E, B> v2) {
    return product(v1, v2, identity());
  }

  static <E, F, A, B> Validator<F, Tuple2<A, B>> product(Validator<E, A> v1,
                                                         Validator<E, B> v2,
                                                         Function1<Sequence<E>, F> reduce) {
    requireNonNull(v1);
    requireNonNull(v2);
    requireNonNull(reduce);
    return value -> map2(
        v1.validate(value.get1()),
        v2.validate(value.get2()),
        Function2.cons(value))
        .mapError(reduce);
  }

  static <E, A, B, C> Validator<Sequence<E>, Tuple3<A, B, C>> product(Validator<E, A> v1,
                                                                      Validator<E, B> v2,
                                                                      Validator<E, C> v3) {
    return product(v1, v2, v3, identity());
  }

  static <E, F, A, B, C> Validator<F, Tuple3<A, B, C>> product(Validator<E, A> v1,
                                                               Validator<E, B> v2,
                                                               Validator<E, C> v3,
                                                               Function1<Sequence<E>, F> reduce) {
    requireNonNull(v1);
    requireNonNull(v2);
    requireNonNull(v3);
    requireNonNull(reduce);
    return value -> map3(
        v1.validate(value.get1()),
        v2.validate(value.get2()),
        v3.validate(value.get3()),
        Function3.cons(value))
        .mapError(reduce);
  }

  static <E, A, B, C, D> Validator<Sequence<E>, Tuple4<A, B, C, D>> product(Validator<E, A> v1,
                                                                            Validator<E, B> v2,
                                                                            Validator<E, C> v3,
                                                                            Validator<E, D> v4) {
    return product(v1, v2, v3, v4, identity());
  }

  static <E, F, A, B, C, D> Validator<F, Tuple4<A, B, C, D>> product(Validator<E, A> v1,
                                                                     Validator<E, B> v2,
                                                                     Validator<E, C> v3,
                                                                     Validator<E, D> v4,
                                                                     Function1<Sequence<E>, F> reduce) {
    requireNonNull(v1);
    requireNonNull(v2);
    requireNonNull(v3);
    requireNonNull(v4);
    requireNonNull(reduce);
    return value -> map4(
        v1.validate(value.get1()),
        v2.validate(value.get2()),
        v3.validate(value.get3()),
        v4.validate(value.get4()),
        Function4.cons(value))
        .mapError(reduce);
  }

  static <F, A, B, C, D, E> Validator<Sequence<F>, Tuple5<A, B, C, D, E>> product(Validator<F, A> v1,
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
                                                                           Function1<Sequence<F>, G> reduce) {
    requireNonNull(v1);
    requireNonNull(v2);
    requireNonNull(v3);
    requireNonNull(v4);
    requireNonNull(v5);
    requireNonNull(reduce);
    return value -> map5(
        v1.validate(value.get1()),
        v2.validate(value.get2()),
        v3.validate(value.get3()),
        v4.validate(value.get4()),
        v5.validate(value.get5()),
        Function5.cons(value))
        .mapError(reduce);
  }

  static <E, T> Validator<Sequence<E>, T> combine(Validator<E, T> v1,
                                                  Validator<E, T> v2) {
    return combine(v1, v2, identity());
  }

  static <E, F, T> Validator<F, T> combine(Validator<E, T> v1,
                                           Validator<E, T> v2,
                                           Function1<Sequence<E>, F> reduce) {
    requireNonNull(v1);
    requireNonNull(v2);
    requireNonNull(reduce);
    return value -> map2(
        v1.validate(value),
        v2.validate(value),
        Function2.cons(value))
        .mapError(reduce);
  }

  static <E, T> Validator<Sequence<E>, T> combine(Validator<E, T> v1,
                                                  Validator<E, T> v2,
                                                  Validator<E, T> v3) {
    return combine(v1, v2, v3, identity());
  }

  static <E, F, T> Validator<F, T> combine(Validator<E, T> v1,
                                           Validator<E, T> v2,
                                           Validator<E, T> v3,
                                           Function1<Sequence<E>, F> reduce) {
    return combine(combine(v1, v2), v3.mapError(Sequence::listOf), Validator.<E>flatten().andThen(reduce));
  }

  static <E, T> Validator<Sequence<E>, T> combine(Validator<E, T> v1,
                                                  Validator<E, T> v2,
                                                  Validator<E, T> v3,
                                                  Validator<E, T> v4) {
    return combine(v1, v2, v3, v4, identity());
  }

  static <E, F, T> Validator<F, T> combine(Validator<E, T> v1,
                                           Validator<E, T> v2,
                                           Validator<E, T> v3,
                                           Validator<E, T> v4,
                                           Function1<Sequence<E>, F> reduce) {
    return combine(combine(v1, v2, v3), v4.mapError(Sequence::listOf), Validator.<E>flatten().andThen(reduce));
  }

  static <E, T> Validator<Sequence<E>, T> combine(Validator<E, T> v1,
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
                                           Function1<Sequence<E>, F> reduce) {
    return combine(combine(v1, v2, v3, v4), v5.mapError(Sequence::listOf), Validator.<E>flatten().andThen(reduce));
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
    return from(value -> value.toUpperCase().equals(value), message);
  }

  static Validator<String, String> lower() {
    return lower(() -> "require lowercase string");
  }

  static Validator<String, String> lower(Producer<String> message) {
    return from(value -> value.toLowerCase().equals(value), message);
  }

  static Validator<String, String> startsWith(String prefix) {
    return startsWith(prefix, () -> "require start with: " + prefix);
  }

  static Validator<String, String> startsWith(String prefix, Producer<String> message) {
    if (isNull(prefix)) {
      throw new IllegalArgumentException("prefix should not be null");
    }
    return from(value -> value.startsWith(prefix), message);
  }

  static Validator<String, String> contains(String substring) {
    return contains(substring, () -> "require contain string: " + substring);
  }

  static Validator<String, String> contains(String substring, Producer<String> message) {
    if (isNull(substring)) {
      throw new IllegalArgumentException("string should not be null");
    }
    return from(value -> value.contains(substring), message);
  }

  static Validator<String, String> endsWith(String suffix) {
    return endsWith(suffix, () -> "require end with: " + suffix);
  }

  static Validator<String, String> endsWith(String suffix, Producer<String> message) {
    if (isNull(suffix)) {
      throw new IllegalArgumentException("suffix should not be null");
    }
    return from(value -> value.endsWith(suffix), message);
  }

  static Validator<String, String> match(String regex) {
    return match(regex, () -> "should match expresion: " + regex);
  }

  static Validator<String, String> match(String regex, Producer<String> message) {
    if (isNull(regex)) {
      throw new IllegalArgumentException("regex should not be null");
    }
    return from(value -> Pattern.matches(regex, value), message);
  }

  static Validator<String, String> minLength(int length) {
    return minLength(length, () -> "require min length: " + length);
  }

  static Validator<String, String> minLength(int length, Producer<String> message) {
    if (length < 0) {
      throw new IllegalArgumentException("length should be a positive value");
    }
    return minValue(length, message).compose(String::length);
  }

  static Validator<String, String> maxLength(int length) {
    return maxLength(length, () -> "require max length: " + length);
  }

  static Validator<String, String> maxLength(int length, Producer<String> message) {
    if (length < 0) {
      throw new IllegalArgumentException("length should be a positive value");
    }
    return maxValue(length, message).compose(String::length);
  }

  static Validator<String, Integer> positive() {
    return minValue(0);
  }

  static Validator<String, Integer> positive(Producer<String> message) {
    return minValue(0, message);
  }

  static Validator<String, Integer> negative() {
    return maxValue(0);
  }

  static Validator<String, Integer> negative(Producer<String> message) {
    return maxValue(0, message);
  }

  static Validator<String, Integer> minValue(int start) {
    return minValue(start, () -> "require min value: " + start);
  }

  static Validator<String, Integer> minValue(int start, Producer<String> message) {
    return from(value -> value >= start, message);
  }

  static Validator<String, Integer> maxValue(int end) {
    return maxValue(end, () -> "require max value: " + end);
  }

  static Validator<String, Integer> maxValue(int end, Producer<String> message) {
    return from(value -> value < end, message);
  }

  static Validator<String, String> length(int start, int end) {
    return length(start, end, Producer.cons(""));
  }

  static Validator<String, String> length(int start, int end, Producer<String> message) {
    if (start >= end) {
      throw new IllegalArgumentException("start should not be greater than end");
    }
    return Validator.<String>nonNull()
        .orElse(combine(minLength(start), maxLength(end), join(message)));
  }

  static Validator<String, Integer> range(int start, int end) {
    return range(start, end, Producer.cons(""));
  }

  static Validator<String, Integer> range(int start, int end, Producer<String> message) {
    if (start >= end) {
      throw new IllegalArgumentException("start should not be greater than end");
    }
    return Validator.<Integer>nonNull()
        .orElse(combine(minValue(start), maxValue(end), join(message)));
  }

  static <E> Function1<Sequence<Sequence<E>>, Sequence<E>> flatten() {
    return seqOfSeqs -> seqOfSeqs.flatMap(identity());
  }

  static <E> Function1<Sequence<E>, String> join() {
    return join(",");
  }

  static <E> Function1<Sequence<E>, String> join(String separator) {
    return seq -> seq.join(separator);
  }

  static <E> Function1<Sequence<E>, String> join(Producer<String> message) {
    return join(",", message);
  }

  static <E> Function1<Sequence<E>, String> join(String separator, Producer<String> message) {
    return seq -> seq.join(separator, message.get(), "");
  }
}
