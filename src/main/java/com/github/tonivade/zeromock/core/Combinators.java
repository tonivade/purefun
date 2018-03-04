/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import tonivade.equalizer.Equalizer;

public final class Combinators {
  
  private Combinators() {}

  public static Function<HttpRequest, HttpRequest> identity() {
    return Function.identity();
  }
  
  public static <H, T, U, R> Function<H, BiTupple<T, U>> join(Function<H, T> beginT, Function<H, U> beginU) {
    return request -> BiTupple.of(beginT.apply(request), beginU.apply(request));
  }
  
  public static <T, U, R> Function<BiTupple<T, U>, R> split(BiFunction<T, U, R> function) {
    return tupple -> function.apply(tupple.get1(), tupple.get2());
  }
  
  public static <T, R> Function<T, R> force(Supplier<R> supplier) {
    return value -> supplier.get();
  }
  
  public static <T, R> Function<T, R> force(Consumer<T> consumer) {
    return value -> { consumer.accept(value); return null; };
  }

  public static <T, R> Function<Optional<T>, Optional<R>> map(Function<T, R> mapper) {
    return optional -> optional.map(mapper);
  }

  public static <T, R> Function<Optional<T>, Optional<R>> flatMap(Function<T, Optional<R>> mapper) {
    return optional -> optional.flatMap(mapper);
  }

  public static <T> Function<Optional<T>, T> orElse(Supplier<T> supplier) {
    return optional -> optional.orElseGet(supplier);
  }

  public static final class BiTupple<T, U> {
    private final T t;
    private final U u;

    private BiTupple(T t, U u) {
      this.t = t;
      this.u = u;
    }
    
    public T get1() {
      return t;
    }
    
    public U get2() {
      return u;
    }
    
    public static <T, U> BiTupple<T, U> of(T t, U u) {
      return new BiTupple<T, U>(t, u);
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(t, u);
    }
    
    @Override
    public boolean equals(Object obj) {
      return Equalizer.equalizer(this)
          .append((a, b) -> Objects.equals(a.t, b.t))
          .append((a, b) -> Objects.equals(a.u, b.u))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "BiTupple(" + t + ", " + u + ")";
    }
  }
}
