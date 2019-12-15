/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Validator.nonEmpty;
import static com.github.tonivade.purefun.Validator.nonNullAnd;
import static com.github.tonivade.purefun.Validator.positive;
import static com.github.tonivade.purefun.data.Sequence.listOf;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Validator;
import com.github.tonivade.purefun.data.NonEmptyList;

/**
 * <p>This type represents the validity or not of a value. There are two possible values:</p>
 * <ul>
 *   <li>{@code Validation.valid(value)}: when the value is valid</li>
 *   <li>{@code Validation.invalid(error)}: when the value is invalid</li>
 * </ul>
 * <p>You can combine different values using mapN methods. Only when all values are valid, the
 * final method is invoked, otherwise a combination of all errors is returned</p>
 * @param <E> type of the error when invalid
 * @param <T> type of the value when valid
 */
@HigherKind
public interface Validation<E, T> {

  static <E, T> Validation<E, T> valid(T value) {
    return new Valid<>(value);
  }

  static <E, T> Validation<E, T> invalid(E error) {
    return new Invalid<>(error);
  }

  static <E, T> Validation<Result<E>, T> invalidOf(E error, E... errors) {
    return new Invalid<>(Result.of(error, errors));
  }

  boolean isValid();
  boolean isInvalid();

  /**
   * Returns the valid value if available. If not, it throws {@code NoSuchElementException}
   * @return the valid value
   * @throws NoSuchElementException if value is not available
   */
  T get();
  /**
   * Returns the invalid value if available. If not, it throws {@code NoSuchElementException}
   * @return the invalid value
   * @throws NoSuchElementException if value is not available
   */
  E getError();

  default <R> Validation<E, R> map(Function1<T, R> mapper) {
    if (isValid()) {
      return valid(mapper.apply(get()));
    }
    return invalid(getError());
  }

  default <U> Validation<U, T> mapError(Function1<E, U> mapper) {
    if (isInvalid()) {
      return invalid(mapper.apply(getError()));
    }
    return valid(get());
  }

  default <R> Validation<E, R> flatMap(Function1<T, Validation<E, R>> mapper) {
    if (isValid()) {
      return mapper.apply(get());
    }
    return invalid(getError());
  }

  default Option<Validation<E, T>> filter(Matcher1<T> matcher) {
    if (isInvalid() || matcher.match(get())) {
      return Option.some(this);
    }
    return Option.none();
  }

  default Option<Validation<E, T>> filterNot(Matcher1<T> matcher) {
    return filter(matcher.negate());
  }

  default Validation<E, T> filterOrElse(Matcher1<T> matcher, Producer<Validation<E, T>> orElse) {
    if (isInvalid() || matcher.match(get())) {
      return this;
    }
    return orElse.get();
  }

  default Validation<E, T> orElse(Validation<E, T> orElse) {
    if (isInvalid()) {
      return orElse;
    }
    return this;
  }

  default T getOrElse(T value) {
    return getOrElse(Producer.cons(value));
  }

  default T getOrElse(Producer<T> orElse) {
    return fold(orElse.asFunction(), identity());
  }

  default T getOrElseThrow() {
    return getOrElseThrow(error -> new IllegalArgumentException(error.toString()));
  }

  default <X extends Throwable> T getOrElseThrow(Function1<E, X> mapper) throws X {
    if (isInvalid()) {
      throw mapper.apply(getError());
    }
    return get();
  }

  default <U> U fold(Function1<E, U> invalidMap, Function1<T, U> validMap) {
    if (isValid()) {
      return validMap.apply(get());
    }
    return invalidMap.apply(getError());
  }

  default <R> Validation<Result<E>, R> ap(Validation<Result<E>, Function1<T, R>> other) {
    if (this.isValid() && other.isValid()) {
      return valid(other.get().apply(get()));
    }
    if (this.isInvalid() && other.isInvalid()) {
      return invalid(other.getError().append(getError()));
    }
    if (this.isInvalid() && other.isValid()) {
      return invalid(Result.of(getError()));
    }
    return invalid(other.getError());
  }

  default Either<E, T> toEither() {
    return fold(Either::left, Either::right);
  }

  ValidationModule module();

  static <E, T, R> Validation<E, R> select(Validation<E, Either<T, R>> validation,
                                           Validation<E, Function1<T, R>> apply) {
    return validation.fold(Validation::invalid,
        either -> either.fold(t -> apply.map(f -> f.apply(t)), Validation::valid));
  }

  static <E, T1, T2, R> Validation<Result<E>, R> map2(Validation<E, T1> validation1,
                                                      Validation<E, T2> validation2,
                                                      Function2<T1, T2, R> mapper) {
    return validation2.ap(validation1.ap(valid(mapper.curried())));
  }

  static <E, T1, T2, T3, R> Validation<Result<E>, R> map3(Validation<E, T1> validation1,
                                                          Validation<E, T2> validation2,
                                                          Validation<E, T3> validation3,
                                                          Function3<T1, T2, T3, R> mapper) {
    return validation3.ap(map2(validation1, validation2, (t1, t2) -> mapper.curried().apply(t1).apply(t2)));
  }

  static <E, T1, T2, T3, T4, R> Validation<Result<E>, R> map4(Validation<E, T1> validation1,
                                                              Validation<E, T2> validation2,
                                                              Validation<E, T3> validation3,
                                                              Validation<E, T4> validation4,
                                                              Function4<T1, T2, T3, T4, R> mapper) {
    return validation4.ap(map3(validation1, validation2, validation3,
        (t1, t2, t3) -> mapper.curried().apply(t1).apply(t2).apply(t3)));
  }

  static <E, T1, T2, T3, T4, T5, R> Validation<Result<E>, R> map5(Validation<E, T1> validation1,
                                                                  Validation<E, T2> validation2,
                                                                  Validation<E, T3> validation3,
                                                                  Validation<E, T4> validation4,
                                                                  Validation<E, T5> validation5,
                                                                  Function5<T1, T2, T3, T4, T5, R> mapper) {
    return validation5.ap(map4(validation1, validation2, validation3, validation4,
        (t1, t2, t3, t4) -> mapper.curried().apply(t1).apply(t2).apply(t3).apply(t4)));
  }

  static <T> Validation<String, T> requireNonNull(T value) {
    return Validator.<T>nonNull().validate(value);
  }

  static Validation<String, String> requireNonEmpty(String value) {
    return nonNullAnd(nonEmpty()).validate(value);
  }

  static Validation<String, Integer> requirePositive(Integer value) {
    return nonNullAnd(positive()).validate(value);
  }

  final class Valid<E, T> implements Validation<E, T>, Serializable {

    private static final long serialVersionUID = -4276395187736455243L;

    private final T value;

    private Valid(T value) {
      this.value = Objects.requireNonNull(value);
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public boolean isInvalid() {
      return false;
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public E getError() {
      throw new NoSuchElementException("valid value");
    }

    @Override
    public ValidationModule module() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .comparing(Valid::get)
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "Valid(" + value + ")";
    }
  }

  final class Invalid<E, T> implements Validation<E, T>, Serializable {

    private static final long serialVersionUID = -5116403366555721062L;

    private final E error;

    private Invalid(E error) {
      this.error = Objects.requireNonNull(error);
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public boolean isInvalid() {
      return true;
    }

    @Override
    public T get() {
      throw new NoSuchElementException("invalid value");
    }

    @Override
    public E getError() {
      return error;
    }

    @Override
    public ValidationModule module() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return Objects.hash(error);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .comparing(Invalid::getError)
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "Invalid(" + error + ")";
    }
  }

  final class Result<E> implements Iterable<E> {

    private final NonEmptyList<E> errors;

    private Result(NonEmptyList<E> errors) {
      this.errors = Objects.requireNonNull(errors);
    }

    public Result<E> append(E error) {
      return new Result<>(errors.append(error));
    }

    public Result<E> appendAll(E... errors) {
      return new Result<>(this.errors.appendAll(listOf(errors)));
    }

    public String join() {
      return join(",");
    }

    public String join(String separator) {
      return errors.join(separator);
    }

    public String join(Producer<String> message) {
      return join(",", message);
    }

    public String join(String separator, Producer<String> message) {
      return errors.join(separator, message.get(), "");
    }

    @Override
    public Iterator<E> iterator() {
      return errors.iterator();
    }

    @SafeVarargs
    public static <E> Result<E> of(E error, E... errors) {
      return new Result<>(NonEmptyList.of(error, errors));
    }

    @Override
    public int hashCode() {
      return Objects.hash(errors);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this).comparing(r -> r.errors).applyTo(obj);
    }

    @Override
    public String toString() {
      return "Result(" + errors.toList() + ")";
    }

    public static <E> Function1<Result<Result<E>>, Result<E>> flatten() {
      return result -> new Result<>(result.errors.flatMap(r -> r.errors));
    }
  }
}

interface ValidationModule {}
