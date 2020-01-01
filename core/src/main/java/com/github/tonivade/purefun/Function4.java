/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

/**
 * <p>This interface represents a function with a four parameters. There's no equivalence in the JVM.</p>
 * <p>The function can throws checked exceptions, but calling {@code apply()} method, the exception is sneaky thrown. So, it
 * can be used as a higher order function in {@link java.util.stream.Stream} or {@link java.util.Optional} API.</p>
 * @param <A> type of first function parameter
 * @param <B> type of second function parameter
 * @param <C> type of third function parameter
 * @param <D> type of fourth function parameter
 * @param <R> type of return value
 */
@FunctionalInterface
public interface Function4<A, B, C, D, R> extends Recoverable {

  default R apply(A a, B b, C c, D d) {
    try {
      return run(a, b, c, d);
    } catch (Throwable t) {
      return sneakyThrow(t);
    }
  }

  R run(A a, B b, C c, D d) throws Throwable;

  default Function1<A, Function1<B, Function1<C, Function1<D, R>>>> curried() {
    return a -> b -> c -> d -> apply(a, b, c, d);
  }

  default Function1<Tuple4<A, B, C, D>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2(), tuple.get3(), tuple.get4());
  }

  default <E> Function4<A, B, C, D, E> andThen(Function1<R, E> after) {
    return (a, b, c, d) -> after.apply(apply(a, b, c, d));
  }

  default <E> Function1<E, R> compose(Function1<E, A> beforeT1, Function1<E, B> beforeT2,
      Function1<E, C> beforeT3, Function1<E, D> beforeT4) {
    return value -> apply(beforeT1.apply(value), beforeT2.apply(value), beforeT3.apply(value), beforeT4.apply(value));
  }

  default Function4<A, B, C, D, R> memoized() {
    return (a, b, c, d) -> new MemoizedFunction<>(tupled()).apply(Tuple.of(a, b, c, d));
  }

  static <A, B, C, D, R> Function4<A, B, C, D, R> cons(R value) {
    return (a, b, c, d) -> value;
  }
}

