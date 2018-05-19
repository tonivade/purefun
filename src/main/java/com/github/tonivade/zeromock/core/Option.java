/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static tonivade.equalizer.Equalizer.equalizer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class Option<T> {
  
  private Option() { }
  
  public static <T> Option<T> some(T value) {
    return new Some<>(value);
  }
  
  public static <T> Option<T> none() {
    return new None<>();
  }

  public static <T> Option<T> of(Supplier<T> supplier) {
    T value = supplier.get();
    if (nonNull(value)) {
      return some(value);
    }
    return none();
  }
  
  public abstract T get();
  public abstract boolean isPresent();
  public abstract boolean isEmpty();
  
  @SuppressWarnings("unchecked")
  public <R> Option<R> map(Handler1<T, R> map) {
    if (isPresent()) {
      return some(map.handle(get()));
    }
    return (Option<R>) this;
  }

  @SuppressWarnings("unchecked")
  public <R> Option<R> flatMap(Handler1<T, Option<R>> map) {
    if (isPresent()) {
      return map.handle(get());
    }
    return (Option<R>) this;
  }

  public Option<T> ifPresent(Consumer<T> consumer) {
    if (isPresent()) {
      consumer.accept(get());
    }
    return this;
  }

  public Option<T> filter(Matcher<T> matcher) {
    if (isPresent() && matcher.match(get())) {
      return this;
    }
    return none();
  }

  public T orElse(Supplier<T> supplier) {
    if (isEmpty()) {
      return supplier.get();
    }
    return get();
  }

  public Stream<T> stream() {
    if (isPresent()) {
      return Stream.of(get());
    }
    return Stream.empty();
  }
  
  public Optional<T> toOptional() {
    if (isPresent()) {
      return Optional.of(get());
    }
    return Optional.empty();
  }

  static final class Some<T> extends Option<T> {
    private final T value;
    
    public Some(T value) {
      this.value = requireNonNull(value);
    }
    
    @Override
    public T get() {
      return value;
    }
    
    @Override
    public boolean isEmpty() {
      return false;
    }
    
    @Override
    public boolean isPresent() {
      return true;
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
      return "Some(" + value + ")";
    }
  }

  static final class None<T> extends Option<T> {
    @Override
    public T get() {
      throw new IllegalStateException();
    }
    
    @Override
    public boolean isEmpty() {
      return true;
    }
    
    @Override
    public boolean isPresent() {
      return false;
    }
    
    @Override
    public int hashCode() {
      return 1;
    }
    
    @Override
    public boolean equals(Object obj) {
      return equalizer(this).applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "None";
    }
  }
}
