/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

/**
 * <p>This interface represents a function with a three parameters. There's no equivalence in the JVM.</p>
 * <p>The function can throws checked exceptions, but calling {@code apply()} method, the exception is sneaky thrown. So, it
 * can be used as a higher order function in {@link java.util.stream.Stream} or {@link java.util.Optional} API.</p>
 * @param <A> type of first function parameter
 * @param <B> type of second function parameter
 * @param <C> type of third function parameter
 * @param <R> type of return value
 */
@FunctionalInterface
public interface Function3<A, B, C, R> extends Recoverable {

  default R apply(A a, B b, C c) {
    try {
      return run(a, b, c);
    } catch (Throwable t) {
      return sneakyThrow(t);
    }
  }

  R run(A a, B b, C c) throws Throwable;

  default Function1<A, Function1<B, Function1<C, R>>> curried() {
    return a -> b -> c -> apply(a, b, c);
  }

  default Function1<Tuple3<A, B, C>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2(), tuple.get3());
  }

  default <D> Function3<A, B, C, D> andThen(Function1<? super R, ? extends D> after) {
    return (a, b, c) -> after.apply(apply(a, b, c));
  }

  default <D> Function1<D, R> compose(Function1<? super D, ? extends A> beforeT1, Function1<? super D, ? extends B> beforeT2, Function1<? super D, ? extends C> beforeT3) {
    return value -> apply(beforeT1.apply(value), beforeT2.apply(value), beforeT3.apply(value));
  }

  default Function3<A, B, C, R> memoized() {
    return (a, b, c) -> new MemoizedFunction<>(tupled()).apply(Tuple.of(a, b, c));
  }

  static <A, B, C, R> Function3<A, B, C, R> cons(R value) {
    return (a, b, c) -> value;
  }

  static <A, B, C> Function3<A, B, C, A> first() {
    return (a, b, c) -> a;
  }

  static <A, B, C> Function3<A, B, C, B> second() {
    return (a, b, c) -> b;
  }

  static <A, B, C> Function3<A, B, C, C> third() {
    return (a, b, c) -> c;
  }
}
