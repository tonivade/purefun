/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

/**
 * This interface represents a function without any parameter. Similar to {@link java.util.function.Supplier}
 * but with additional functionality like the ability to memoize the result.
 * @param <T> the returned type
 */
@HigherKind
@FunctionalInterface
public interface Producer<T> extends Recoverable {

  default T get() {
    try {
      return run();
    } catch (Throwable t) {
      return sneakyThrow(t);
    }
  }

  T run() throws Throwable;

  default <V> Function1<V, T> asFunction() {
    return value -> run();
  }

  default <R> Producer<R> andThen(Function1<T, R> after) {
    return map(after);
  }

  default <R> Producer<R> map(Function1<T, R> after) {
    return () -> after.apply(get());
  }

  default <R> Producer<R> flatMap(Function1<T, Producer<R>> after) {
    return () -> after.apply(get()).get();
  }

  default Producer<Option<T>> liftOption() {
    return map(Option::of);
  }

  default Producer<Try<T>> liftTry() {
    return () -> Try.of(this::get);
  }

  default Producer<Either<Throwable, T>> liftEither() {
    return liftTry().map(Try::toEither);
  }

  default Producer<T> memoized() {
    return new MemoizedProducer<>(this);
  }

  static <T> Producer<T> cons(T value) {
    return () -> value;
  }

  static <A, X extends Throwable> Producer<A> failure(Producer<X> supplier) {
    return () -> { throw supplier.get(); };
  }

  static <T> Producer<T> of(Producer<T> reference) {
    return reference;
  }
}
