/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Validator.greaterThan;
import static com.github.tonivade.purefun.core.Validator.greaterThanOrEqual;
import static com.github.tonivade.purefun.core.Validator.lowerThan;
import static com.github.tonivade.purefun.core.Validator.lowerThanOrEqual;
import static com.github.tonivade.purefun.core.Validator.nonEmpty;
import static com.github.tonivade.purefun.core.Validator.nonNullAnd;
import static com.github.tonivade.purefun.core.Validator.positive;
import static com.github.tonivade.purefun.data.Sequence.listOf;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Function3;
import com.github.tonivade.purefun.core.Function4;
import com.github.tonivade.purefun.core.Function5;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Validator;
import com.github.tonivade.purefun.data.ImmutableList;
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
public sealed interface Validation<E, T> extends ValidationOf<E, T>, Bindable<Kind<Validation_, E>, T> {

  static <E, T> Validation<E, T> valid(T value) {
    return new Valid<>(value);
  }

  static <E, T> Validation<E, T> invalid(E error) {
    return new Invalid<>(error);
  }

  @SafeVarargs
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

  @Override
  @SuppressWarnings("unchecked")
  default <R> Validation<E, R> map(Function1<? super T, ? extends R> mapper) {
    if (this instanceof Valid<E, T>(var value)) {
      return valid(mapper.apply(value));
    }
    return (Validation<E, R>) this;
  }

  @SuppressWarnings("unchecked")
  default <U> Validation<U, T> mapError(Function1<? super E, ? extends U> mapper) {
    if (this instanceof Invalid<E, T>(var error)) {
      return invalid(mapper.apply(error));
    }
    return (Validation<U, T>) this;
  }

  default <U, R>  Validation<U, R> bimap(Function1<? super E, ? extends U> error, Function1<? super T, ? extends R> mapper) {
    Validation<U, T> mapError = mapError(error);
    return mapError.map(mapper);
  }

  @Override
  @SuppressWarnings("unchecked")
  default <R> Validation<E, R> flatMap(Function1<? super T, ? extends Kind<Kind<Validation_, E>, ? extends R>> mapper) {
    if (this instanceof Valid<E, T>(var value)) {
      return mapper.andThen(ValidationOf::<E, R>narrowK).apply(value);
    }
    return (Validation<E, R>) this;
  }

  default Option<Validation<E, T>> filter(Matcher1<? super T> matcher) {
    if (this instanceof Invalid) {
      return Option.some(this);
    }
    if (this instanceof Valid<E, T>(var value) && matcher.match(value)) {
      return Option.some(this);
    }
    return Option.none();
  }

  default Option<Validation<E, T>> filterNot(Matcher1<? super T> matcher) {
    return filter(matcher.negate());
  }

  default Validation<E, T> filterOrElse(Matcher1<? super T> matcher, Producer<? extends Kind<Kind<Validation_, E>, T>> orElse) {
    if (this instanceof Invalid) {
      return this;
    }
    if (this instanceof Valid<E, T>(var value) && matcher.match(value)) {
      return this;
    }
    return orElse.andThen(ValidationOf::narrowK).get();
  }

  default Validation<E, T> or(Producer<Kind<Kind<Validation_, E>, T>> orElse) {
    if (this instanceof Invalid) {
      return orElse.andThen(ValidationOf::narrowK).get();
    }
    return this;
  }

  default Validation<E, T> orElse(Kind<Kind<Validation_, E>, T> orElse) {
    return or(Producer.cons(orElse));
  }

  default T getOrElse(T value) {
    return getOrElse(Producer.cons(value));
  }

  default T getOrElseNull() {
    return getOrElse(Producer.cons(null));
  }

  default T getOrElse(Producer<? extends T> orElse) {
    return fold(orElse.asFunction(), identity());
  }

  default T getOrElseThrow() {
    return getOrElseThrow(error -> new IllegalArgumentException(error.toString()));
  }

  default <X extends Throwable> T getOrElseThrow(Function1<? super E, ? extends X> mapper) throws X {
    if (this instanceof Valid<E, T>(var value)) {
      return value;
    }
    throw mapper.apply(getError());
  }

  default <U> U fold(Function1<? super E, ? extends U> invalidMap, Function1<? super T, ? extends U> validMap) {
    if (this instanceof Valid<E, T>(var value)) {
      return validMap.apply(value);
    }
    if (this instanceof Invalid<E, T>(var error)) {
      return invalidMap.apply(error);
    }
    throw new UnsupportedOperationException();
  }

  default <R> Validation<Result<E>, R> ap(Validation<Result<E>, Function1<? super T, ? extends R>> other) {
    if (this.isValid() && other.isValid()) {
      return valid(other.getOrElseThrow().apply(getOrElseThrow()));
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

  static <E, T, R> Validation<E, R> select(Validation<E, Either<T, R>> validation,
                                           Validation<E, Function1<? super T, ? extends R>> apply) {
    return validation.fold(Validation::invalid,
        either -> either.fold(t -> apply.map(f -> f.apply(t)), Validation::valid));
  }

  static <E, T1, T2, R> Validation<Result<E>, R> mapN(Validation<E, T1> validation1,
                                                      Validation<E, T2> validation2,
                                                      Function2<? super T1, ? super T2, ? extends R> mapper) {
    return validation2.ap(validation1.ap(valid(mapper.curried())));
  }

  static <E, T1, T2, T3, R> Validation<Result<E>, R> mapN(Validation<E, T1> validation1,
                                                          Validation<E, T2> validation2,
                                                          Validation<E, T3> validation3,
                                                          Function3<? super T1, ? super T2, ? super T3, ? extends R> mapper) {
    return validation3.ap(mapN(validation1, validation2, (t1, t2) -> mapper.curried().apply(t1).apply(t2)));
  }

  static <E, T1, T2, T3, T4, R> Validation<Result<E>, R> mapN(Validation<E, T1> validation1,
                                                              Validation<E, T2> validation2,
                                                              Validation<E, T3> validation3,
                                                              Validation<E, T4> validation4,
                                                              Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> mapper) {
    return validation4.ap(mapN(validation1, validation2, validation3,
        (t1, t2, t3) -> mapper.curried().apply(t1).apply(t2).apply(t3)));
  }

  static <E, T1, T2, T3, T4, T5, R> Validation<Result<E>, R> mapN(Validation<E, T1> validation1,
                                                                  Validation<E, T2> validation2,
                                                                  Validation<E, T3> validation3,
                                                                  Validation<E, T4> validation4,
                                                                  Validation<E, T5> validation5,
                                                                  Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> mapper) {
    return validation5.ap(mapN(validation1, validation2, validation3, validation4,
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

  static Validation<String, Integer> requireGreaterThan(Integer value, int x) {
    return nonNullAnd(greaterThan(x, () -> "require " + value + " > " + x)).validate(value);
  }

  static Validation<String, Integer> requireGreaterThanOrEqual(Integer value, int x) {
    return nonNullAnd(greaterThanOrEqual(x, () -> "require " + value + " >= " + x)).validate(value);
  }

  static Validation<String, Integer> requireLowerThan(Integer value, int x) {
    return nonNullAnd(lowerThan(x, () -> "require " + value + " < " + x)).validate(value);
  }

  static Validation<String, Integer> requireLowerThanOrEqual(Integer value, int x) {
    return nonNullAnd(lowerThanOrEqual(x, () -> "require " + value + " <= " + x)).validate(value);
  }

  record Valid<E, T>(T value) implements Validation<E, T>, Serializable {

    public Valid {
      checkNonNull(value);
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
    public String toString() {
      return "Valid(" + value + ")";
    }
  }

  record Invalid<E, T>(E error) implements Validation<E, T>, Serializable {

    public Invalid {
      checkNonNull(error);
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
    public String toString() {
      return "Invalid(" + error + ")";
    }
  }

  final class Result<E> implements Iterable<E>, Serializable {

    @Serial
    private static final long serialVersionUID = -6528420803580087615L;

    private static final Equal<Result<?>> EQUAL = Equal.<Result<?>>of().comparing(r -> r.errors);

    private final NonEmptyList<E> errors;

    private Result(NonEmptyList<E> errors) {
      this.errors = Objects.requireNonNull(errors);
    }

    public Result<E> append(E error) {
      return new Result<>(errors.append(error));
    }

    @SuppressWarnings("unchecked")
    public Result<E> appendAll(E... other) {
      return new Result<>(this.errors.appendAll(listOf(other)));
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

    public static <E> Result<E> from(Iterable<E> errors) {
      return new Result<>(NonEmptyList.of(ImmutableList.from(errors)));
    }

    @Override
    public int hashCode() {
      return Objects.hash(errors);
    }

    @Override
    public boolean equals(Object obj) {
      return EQUAL.applyTo(this, obj);
    }

    @Override
    public String toString() {
      return "Result(" + errors.toList() + ")";
    }

    public static <E> Result<E> concat(Result<E> a, Result<E> b) {
      return new Result<>(a.errors.appendAll(b.errors));
    }

    public static <E> Function1<Result<Result<E>>, Result<E>> flatten() {
      return result -> new Result<>(result.errors.flatMap(r -> r.errors));
    }
  }
}
