/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Combinators {
  
  private Combinators() {}
  
  public static <T, U, R> Function<HttpRequest, BiTupple<T, U>> join(Function<HttpRequest, T> beginT, 
                                                                     Function<HttpRequest, U> beginU) {
    return request -> new BiTupple<>(beginT.apply(request), beginU.apply(request));
  }
  
  public static <T, U, R> Function<BiTupple<T, U>, R> split(BiFunction<T, U, R> function) {
    return tupple -> function.apply(tupple.get1(), tupple.get2());
  }
  
  public static <T, R> Function<T, R> force(Supplier<R> supplier) {
    return value -> supplier.get();
  }
  
  public static <T> Function<T, Void> force(Consumer<T> consumer) {
    return value -> { consumer.accept(value); return null; };
  }

  public static <T> Function<Optional<T>, T> orElse(Supplier<T> supplier) {
    return optional -> optional.orElseGet(supplier);
  }

  public static <T, R> Function<Optional<T>, Optional<R>> map(Function<T, R> mapper) {
    return optional -> optional.map(mapper);
  }

  private static final class BiTupple<T, U> {
    private final T t;
    private final U u;

    public BiTupple(T t, U u) {
      this.t = t;
      this.u = u;
    }
    
    public T get1() {
      return t;
    }
    
    public U get2() {
      return u;
    }
  }
}
