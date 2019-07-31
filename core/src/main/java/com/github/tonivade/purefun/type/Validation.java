/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
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
import com.github.tonivade.purefun.data.Sequence;

@HigherKind
public interface Validation<E, T> {

  static <E, T> Validation<E, T> valid(T value) {
    return new Valid<>(value);
  }

  static <E, T> Validation<E, T> invalid(E error) {
    return new Invalid<>(error);
  }

  boolean isValid();
  boolean isInvalid();

  T get();
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

  default <U> U fold(Function1<E, U> invalidMap, Function1<T, U> validMap) {
    if (isValid()) {
      return validMap.apply(get());
    }
    return invalidMap.apply(getError());
  }

  default <R> Validation<Sequence<E>, R> ap(Validation<Sequence<E>, Function1<T, R>> other) {
    if (this.isValid() && other.isValid()) {
      return valid(other.get().apply(get()));
    }
    if (this.isInvalid() && other.isInvalid()) {
      return invalid(other.getError().append(getError()));
    }
    if (this.isInvalid() && other.isValid()) {
      return invalid(listOf(getError()));
    }
    return invalid(other.getError());
  }

  default Either<E, T> toEither() {
    return fold(Either::left, Either::right);
  }

  ValidationModule module();

  static <E, T1, T2, R> Validation<Sequence<E>, R> map2(Validation<E, T1> validation1,
                                                        Validation<E, T2> validation2,
                                                        Function2<T1, T2, R> mapper) {
    return validation2.ap(validation1.ap(valid(mapper.curried())));
  }

  static <E, T1, T2, T3, R> Validation<Sequence<E>, R> map3(Validation<E, T1> validation1,
                                                            Validation<E, T2> validation2,
                                                            Validation<E, T3> validation3,
                                                            Function3<T1, T2, T3, R> mapper) {
    return validation3.ap(map2(validation1, validation2, (t1, t2) -> mapper.curried().apply(t1).apply(t2)));
  }

  static <E, T1, T2, T3, T4, R> Validation<Sequence<E>, R> map4(Validation<E, T1> validation1,
                                                                Validation<E, T2> validation2,
                                                                Validation<E, T3> validation3,
                                                                Validation<E, T4> validation4,
                                                                Function4<T1, T2, T3, T4, R> mapper) {
    return validation4.ap(map3(validation1, validation2, validation3,
        (t1, t2, t3) -> mapper.curried().apply(t1).apply(t2).apply(t3)));
  }

  static <E, T1, T2, T3, T4, T5, R> Validation<Sequence<E>, R> map5(Validation<E, T1> validation1,
                                                                    Validation<E, T2> validation2,
                                                                    Validation<E, T3> validation3,
                                                                    Validation<E, T4> validation4,
                                                                    Validation<E, T5> validation5,
                                                                    Function5<T1, T2, T3, T4, T5, R> mapper) {
    return validation5.ap(map4(validation1, validation2, validation3, validation4,
        (t1, t2, t3, t4) -> mapper.curried().apply(t1).apply(t2).apply(t3).apply(t4)));
  }

  final class Valid<E, T> implements Validation<E, T>, Serializable {

    private static final long serialVersionUID = -4276395187736455243L;

    private final T value;

    private Valid(T value) {
      this.value = requireNonNull(value);
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
      this.error = requireNonNull(error);
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
}

interface ValidationModule {}
