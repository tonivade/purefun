/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Objects.requireNonNull;
import static tonivade.equalizer.Equalizer.equalizer;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class Try<T> {
  
  private Try() { }
  
  public static <T> Try<T> success(T value) {
    return new Success<>(value);
  }
  
  public static <T> Try<T> failure(String message) {
    return new Failure<>(new AssertionError(message));
  }
  
  public static <T> Try<T> failure(Throwable error) {
    return new Failure<>(error);
  }
  
  public static <T> Try<T> of(Supplier<T> supplier) {
    try {
      return success(supplier.get());
    } catch (Throwable error) {
      return failure(error);
    }
  }

  public abstract T get();
  public abstract Throwable getCause();
  public abstract boolean isSuccess();
  public abstract boolean isFailure();
  
  @SuppressWarnings("unchecked")
  public <R> Try<R> map(Handler1<T, R> map) {
    if (isSuccess()) {
      return success(map.handle(get()));
    }
    return (Try<R>) this;
  }

  @SuppressWarnings("unchecked")
  public <R> Try<R> flatMap(Handler1<T, Try<R>> map) {
    if (isSuccess()) {
      return map.handle(get());
    }
    return (Try<R>) this;
  }

  public Try<T> onFailure(Consumer<Throwable> consumer) {
    if (isFailure()) {
      consumer.accept(getCause());
    }
    return this;
  }
  
  public Try<T> onSuccess(Consumer<T> consumer) {
    if (isSuccess()) {
      consumer.accept(get());
    }
    return this;
  }

  public Try<T> filter(Predicate<T> predicate) {
    if (isSuccess() && predicate.test(get())) {
      return this;
    }
    return failure("filtered");
  }

  public T orElse(Supplier<T> supplier) {
    if (isFailure()) {
      return supplier.get();
    }
    return get();
  }

  public Stream<T> stream() {
    if (isSuccess()) {
      return Stream.of(get());
    }
    return Stream.empty();
  }
  
  public static class Success<T> extends Try<T> {
    private final T value;
    
    public Success(T value) {
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
      throw new IllegalStateException("success doesn't have any cause");
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return equalizer(this)
          .append((a, b) -> Objects.equals(a.value, b.value))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "Success(" + value + ")";
    }
  }
  
  public static class Failure<T> extends Try<T> {
    private final Throwable cause;
    
    public Failure(Throwable cause) {
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
      throw new IllegalStateException("failure doesn't have any value");
    }
    
    @Override
    public Throwable getCause() {
      return cause;
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(cause.getMessage(), cause.getStackTrace());
    }

    @Override
    public boolean equals(Object obj) {
      return equalizer(this)
          .append((a, b) -> Objects.equals(a.cause.getMessage(), b.cause.getMessage()))
          .append((a, b) -> Arrays.deepEquals(a.cause.getStackTrace(), b.cause.getStackTrace()))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "Failure(" + cause + ")";
    }
  }
}
