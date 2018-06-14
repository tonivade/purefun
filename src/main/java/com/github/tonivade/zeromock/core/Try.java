/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static com.github.tonivade.zeromock.core.Equal.comparingArray;
import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

public interface Try<T> extends Functor<T>, Filterable<T>, Holder<T> {
  
  static <T> Try<T> success(T value) {
    return new Success<>(value);
  }
  
  static <T> Try<T> failure(String message) {
    return failure(new Exception(message));
  }

  static <T> Try<T> failure() {
    return failure(new Exception());
  }
  
  static <T> Try<T> failure(Throwable error) {
    return new Failure<>(error);
  }
  
  static <T> Try<T> of(Producer<T> supplier) {
    try {
      return success(supplier.get());
    } catch (Exception error) {
      return failure(error);
    }
  }

  Throwable getCause();
  boolean isSuccess();
  boolean isFailure();
  
  @Override
  default <R> Try<R> map(Function1<T, R> mapper) {
    if (isSuccess()) {
      return success(mapper.apply(get()));
    }
    return failure(getCause());
  }

  default <R> Try<R> flatMap(Function1<T, Try<R>> mapper) {
    if (isSuccess()) {
      return mapper.apply(get());
    }
    return failure(getCause());
  }

  default Try<T> onFailure(Consumer1<Throwable> consumer) {
    if (isFailure()) {
      consumer.accept(getCause());
    }
    return this;
  }
  
  default Try<T> onSuccess(Consumer1<T> consumer) {
    if (isSuccess()) {
      consumer.accept(get());
    }
    return this;
  }
  
  default Try<T> recover(Function1<Throwable, T> mapper) {
    if (isFailure()) {
      return Try.of(() -> mapper.apply(getCause()));
    }
    return this;
  }
  
  @SuppressWarnings("unchecked")
  default <X extends Throwable> Try<T> recoverWith(Class<X> type, Function1<X, T> mapper) {
    if (isFailure()) {
      Throwable cause = getCause();
      if (type.isAssignableFrom(cause.getClass())) {
        return Try.of(() -> mapper.apply((X) getCause()));
      }
    }
    return this;
  }

  @Override
  default Try<T> filter(Matcher<T> matcher) {
    return filterOrElse(matcher, () -> failure(new NoSuchElementException("filtered")));
  }

  default Try<T> filterOrElse(Matcher<T> matcher, Producer<Try<T>> producer) {
    if (isFailure() || matcher.match(get())) {
      return this;
    }
    return producer.get();
  }
  
  default <U> U fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper) {
    if (isSuccess()) {
      return successMapper.apply(get());
    }
    return failureMapper.apply(getCause());
  }

  default T orElse(T value) {
    return orElse(Producer.unit(value));
  }

  default T orElse(Producer<T> producer) {
    if (isSuccess()) {
      return get();
    }
    return producer.get();
  }

  default Stream<T> stream() {
    if (isSuccess()) {
      return Stream.of(get());
    }
    return Stream.empty();
  }
  
  default Either<Throwable, T> toEither() {
    if (isSuccess()) {
      return Either.right(get());
    }
    return Either.left(getCause());
  }
  
  default <E> Validation<E, T> toValidation(Function1<Throwable, E> map) {
    if (isSuccess()) {
      return Validation.valid(get());
    }
    return Validation.invalid(map.apply(getCause()));
  }
  
  default Option<T> toOption() {
    if (isSuccess()) {
      return Option.some(get());
    }
    return Option.none();
  }
  
  final class Success<T> implements Try<T> {
    private final T value;
    
    private Success(T value) {
      this.value = requireNonNull(value);
    }
    
    @Override
    public boolean isFailure() {
      return false;
    }
    
    @Override
    public boolean isSuccess() {
      return true;
    }
    
    @Override
    public T get() {
      return value;
    }
    
    @Override
    public Throwable getCause() {
      throw new NoSuchElementException("success doesn't have any cause");
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return equal(this)
          .append(comparing(Try::get))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "Success(" + value + ")";
    }
  }
  
  final class Failure<T> implements Try<T> {
    private final Throwable cause;
    
    private Failure(Throwable cause) {
      this.cause = requireNonNull(cause);
    }
    
    @Override
    public boolean isFailure() {
      return true;
    }
    
    @Override
    public boolean isSuccess() {
      return false;
    }
    
    @Override
    public T get() {
      throw new NoSuchElementException("failure doesn't have any value");
    }
    
    @Override
    public Throwable getCause() {
      return cause;
    }
    
    private String getMessage() {
      return cause.getMessage();
    }
    
    private StackTraceElement[] getStackTrace() {
      return cause.getStackTrace();
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(cause.getMessage(), cause.getStackTrace());
    }

    @Override
    public boolean equals(Object obj) {
      return equal(this)
          .append(comparing(Failure::getMessage))
          .append(comparingArray(Failure::getStackTrace))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "Failure(" + cause + ")";
    }
  }
}
