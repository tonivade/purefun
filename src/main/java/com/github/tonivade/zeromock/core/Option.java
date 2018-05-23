/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class Option<T> {
  
  private Option() { }
  
  public static <T> Option<T> some(T value) {
    return new Some<>(value);
  }
  
  public static <T> Option<T> none() {
    return new None<>();
  }

  public static <T> Option<T> of(Handler0<T> supplier) {
    T value = supplier.handle();
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

  public T orElse(Handler0<T> supplier) {
    if (isEmpty()) {
      return supplier.handle();
    }
    return get();
  }
  
  public <U> U fold(Handler0<U> orElse, Handler1<T, U> mapper) {
    if (isPresent()) {
      return mapper.handle(get());
    }
    return orElse.handle();
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
    
    private Some(T value) {
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
      return equal(this)
          .append(comparing(Option::get))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "Some(" + value + ")";
    }
  }

  static final class None<T> extends Option<T> {

    private None() { }

    @Override
    public T get() {
      throw new NoSuchElementException("get() in none");
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
      return equal(this).applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "None";
    }
  }
}
