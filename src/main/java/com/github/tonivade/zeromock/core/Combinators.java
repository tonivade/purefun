/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Objects.requireNonNull;
import static tonivade.equalizer.Equalizer.equalizer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class Combinators {
  
  private Combinators() {}
  
  public static <H, T, U, R> Function<H, BiTupple<T, U>> tupple(Function<H, T> beginT, Function<H, U> beginU) {
    return value -> BiTupple.of(beginT.apply(value), beginU.apply(value));
  }
  
  public static <T, U, R> Function<BiTupple<T, U>, R> apply(BiFunction<T, U, R> function) {
    return tupple -> function.apply(tupple.get1(), tupple.get2());
  }
  
  public static <T, R> Function<T, R> identity(Function<T, R> function) {
    return function;
  }
  
  public static <T, R> Function<T, R> adapt(Supplier<R> supplier) {
    return value -> supplier.get();
  }
  
  public static <T> UnaryOperator<T> adapt(Consumer<T> consumer) {
    return value -> { consumer.accept(value); return value; };
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
  
  public static <T, R> Function<T, Optional<R>> lift(Function<T, R> function) {
    return function.andThen(Optional::of);
  }

  public static final class BiTupple<T, U> {
    private final T t;
    private final U u;

    private BiTupple(T t, U u) {
      this.t = requireNonNull(t);
      this.u = requireNonNull(u);
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
      return equalizer(this)
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
