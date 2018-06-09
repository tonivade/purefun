/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static com.github.tonivade.zeromock.core.Equal.equal;
import static com.github.tonivade.zeromock.core.Sequence.listOf;
import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;
import java.util.Objects;

public interface Validation<E, T> extends Holder<T>, Functor<T> {
  
  static <E, T> Validation<E, T> valid(T value) {
    return new Valid<>(value);
  }
  
  static <E, T> Validation<E, T> invalid(E error) {
    return new Invalid<>(error);
  }
  
  boolean isValid();
  boolean isInvalid();

  E getError();
  
  @Override
  default <R> Validation<E, R> map(Handler1<T, R> map) {
    if (isValid()) {
      return valid(map.handle(get()));
    }
    return invalid(getError());
  }
  
  default <U> Validation<U, T> mapError(Handler1<E, U> map) {
    if (isInvalid()) {
      return invalid(map.handle(getError()));
    }
    return valid(get());
  }
  
  default <R> Validation<E, R> flatMap(Handler1<T, Validation<E, R>> map) {
    if (isValid()) {
      return map.handle(get());
    }
    return invalid(getError());
  }
  
  default Option<Validation<E, T>> filter(Matcher<T> matcher) {
    if (isInvalid() || matcher.match(get())) {
      return Option.some(this);
    }
    return Option.none();
  }
  
  default <U> U fold(Handler1<T, U> validMap, Handler1<E, U> invalidMap) {
    if (isValid()) {
      return validMap.handle(get());
    }
    return invalidMap.handle(getError());
  }
 
  default <R> Validation<Sequence<E>, R> ap(Validation<Sequence<E>, Handler1<T, R>> validation) {
    if (isValid() && validation.isValid()) {
      return valid(validation.get().handle(get()));
    } 
    if (isInvalid() && validation.isInvalid()) {
      return invalid(validation.getError().append(getError()));
    }
    if (isInvalid() && validation.isValid()) {
      return invalid(listOf(getError()));
    }
    return invalid(validation.getError());
  }

  static <E, T1, T2, R> Validation<Sequence<E>, R> map2(Validation<E, T1> validation1, 
                                                        Validation<E, T2> validation2, 
                                                        Handler2<T1, T2, R> map) {
    return validation2.ap(validation1.ap(valid(map.curried())));
  }

  static <E, T1, T2, T3, R> Validation<Sequence<E>, R> map3(Validation<E, T1> validation1, 
      Validation<E, T2> validation2, 
      Validation<E, T3> validation3, 
      Handler1<T1, Handler1<T2, Handler1<T3, R>>> map) {
    return validation3.ap(map2(validation1, validation2, (t1, t2) -> map.handle(t1).handle(t2)));
  }

  static <E, T1, T2, T3, T4, R> Validation<Sequence<E>, R> map4(Validation<E, T1> validation1, 
      Validation<E, T2> validation2, 
      Validation<E, T3> validation3, 
      Validation<E, T4> validation4, 
      Handler1<T1, Handler1<T2, Handler1<T3, Handler1<T4, R>>>> map) {
    return validation4.ap(map3(validation1, validation2, validation3, 
        t1 -> t2 -> t3 -> map.handle(t1).handle(t2).handle(t3)));
  }

  static <E, T1, T2, T3, T4, T5, R> Validation<Sequence<E>, R> map5(Validation<E, T1> validation1, 
      Validation<E, T2> validation2, 
      Validation<E, T3> validation3, 
      Validation<E, T4> validation4, 
      Validation<E, T5> validation5, 
      Handler1<T1, Handler1<T2, Handler1<T3, Handler1<T4, Handler1<T5, R>>>>> map) {
    return validation5.ap(map4(validation1, validation2, validation3, validation4, 
        t1 -> t2 -> t3 -> t4 -> map.handle(t1).handle(t2).handle(t3).handle(t4)));
  }
  
  final class Valid<E, T> implements Validation<E, T> {
    
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
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return equal(this)
          .append(comparing(Valid::get))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "Valid(" + value + ")";
    }
  }
  
  final class Invalid<E, T> implements Validation<E, T> {
    
    private final E error;
    
    public Invalid(E error) {
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
    public int hashCode() {
      return Objects.hash(error);
    }

    @Override
    public boolean equals(Object obj) {
      return equal(this)
          .append(comparing(Invalid::getError))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "Invalid(" + error + ")";
    }
  }
}
