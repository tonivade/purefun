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

public interface Option<T> extends Functor<T>, Filterable<T>, Holder<T> {
  
  static <T> Option<T> some(T value) {
    return new Some<>(value);
  }
  
  @SuppressWarnings("unchecked")
  static <T> Option<T> none() {
    return (Option<T>) None.INSTANCE;
  }

  static <T> Option<T> of(Handler0<T> supplier) {
    T value = supplier.handle();
    if (nonNull(value)) {
      return some(value);
    }
    return none();
  }

  static <T> Option<T> from(Optional<T> optional) {
    return optional.map(Option::some).orElseGet(() -> Option.none());
  }
  
  boolean isPresent();
  boolean isEmpty();
  
  @Override
  default <R> Option<R> map(Handler1<T, R> map) {
    if (isPresent()) {
      return some(map.handle(get()));
    }
    return none();
  }

  default <R> Option<R> flatMap(OptionHandler<T, R> map) {
    if (isPresent()) {
      return map.handle(get());
    }
    return none();
  }

  default Option<T> ifPresent(Consumer<T> consumer) {
    if (isPresent()) {
      consumer.accept(get());
    }
    return this;
  }

  @Override
  default Option<T> filter(Matcher<T> matcher) {
    if (isPresent() && matcher.match(get())) {
      return this;
    }
    return none();
  }

  default T orElse(Handler0<T> supplier) {
    if (isEmpty()) {
      return supplier.handle();
    }
    return get();
  }

  default <X extends Throwable> T orElseThrow(Handler0<X> supplier) throws X { 
    if (isEmpty()) {
      throw supplier.handle();
    }
    return get();
  }
  
  default <U> U fold(Handler0<U> orElse, Handler1<T, U> mapper) {
    if (isPresent()) {
      return mapper.handle(get());
    }
    return orElse.handle();
  }

  default Stream<T> stream() {
    if (isPresent()) {
      return Stream.of(get());
    }
    return Stream.empty();
  }
  
  default Optional<T> toOptional() {
    if (isPresent()) {
      return Optional.of(get());
    }
    return Optional.empty();
  }

  final class Some<T> implements Option<T> {
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

  final class None<T> implements Option<T> {
    
    private static final None<?> INSTANCE = new None<>();

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
      return this == obj;
    }
    
    @Override
    public String toString() {
      return "None";
    }
  }
}
