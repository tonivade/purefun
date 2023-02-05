/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

/**
 * <p>This interface represents a function with a five parameters. There's no equivalence in the JVM.</p>
 * <p>The function can throws checked exceptions, but calling {@code apply()} method, the exception is sneaky thrown. So, it
 * can be used as a higher order function in {@link java.util.stream.Stream} or {@link java.util.Optional} API.</p>
 * @param <A> type of first function parameter
 * @param <B> type of second function parameter
 * @param <C> type of third function parameter
 * @param <D> type of fourth function parameter
 * @param <E> type of fifth function parameter
 * @param <R> type of return value
 */
@FunctionalInterface
public interface Function5<A, B, C, D, E, R> extends Recoverable {

  default R apply(A a, B b, C c, D d, E e) {
    try {
      return run(a, b, c, d, e);
    } catch (Throwable t) {
      return sneakyThrow(t);
    }
  }

  R run(A a, B b, C c, D d, E e) throws Throwable;

  default Function1<A, Function1<B, Function1<C, Function1<D, Function1<E, R>>>>> curried() {
    return a -> b -> c -> d -> e -> apply(a, b, c, d, e);
  }

  default Function1<Tuple5<A, B, C, D, E>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2(), tuple.get3(), tuple.get4(), tuple.get5());
  }

  default <F> Function5<A, B, C, D, E, F> andThen(Function1<? super R, ? extends F> after) {
    return (a, b, c, d, e) -> after.apply(apply(a, b, c, d, e));
  }

  default <F> Function1<F, R> compose(Function1<? super F, ? extends A> beforeT1, Function1<? super F, ? extends B> beforeT2,
      Function1<? super F, ? extends C> beforeT3, Function1<? super F, ? extends D> beforeT4, Function1<? super F, ? extends E> beforeT5) {
    return value -> apply(beforeT1.apply(value), beforeT2.apply(value),
        beforeT3.apply(value), beforeT4.apply(value), beforeT5.apply(value));
  }

  default Function5<A, B, C, D, E, R> memoized() {
    return (a, b, c, d, e) -> new MemoizedFunction<>(tupled()).apply(Tuple.of(a, b, c, d, e));
  }

  static <A, B, C, D, E, R> Function5<A, B, C, D, E, R> cons(R value) {
    return (a, b, c, d, e) -> value;
  }

  static <A, B, C, D, E> Function5<A, B, C, D, E, A> first() {
    return (a, b, c, d, e) -> a;
  }

  static <A, B, C, D, E> Function5<A, B, C, D, E, B> second() {
    return (a, b, c, d, e) -> b;
  }

  static <A, B, C, D, E> Function5<A, B, C, D, E, C> third() {
    return (a, b, c, d, e) -> c;
  }

  static <A, B, C, D, E> Function5<A, B, C, D, E, D> fourth() {
    return (a, b, c, d, e) -> d;
  }

  static <A, B, C, D, E> Function5<A, B, C, D, E, E> fifth() {
    return (a, b, c, d, e) -> e;
  }
}

