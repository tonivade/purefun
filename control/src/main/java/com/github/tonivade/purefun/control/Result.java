/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.core.Recoverable;

public sealed interface Result<T> {

  static <T> T trampoline(Result<T> apply) {
    Result<T> result = apply;

    while (result instanceof Computation<T, ?> current) {
      try {
        result = current.apply();
      } catch (Throwable t) {
        result = current.unwind(t);
      }
    }

    return result.value();
  }

  T value();

  static <T> Result<T> value(T value) {
    return new Value<>(value);
  }

  static <T> Result<T> abort(Throwable error) {
    return new Abort<>(error);
  }

  static <T, R> Result<T> computation(Control<R> control, MetaCont<R, T> cont) {
    return new Computation<>(control, cont);
  }

  final class Value<T> implements Result<T> {

    private final T value;

    private Value(T value) {
      this.value = checkNonNull(value);
    }

    @Override
    public T value() {
      return value;
    }

    @Override
    public String toString() {
      return String.format("Value(%s)", value);
    }
  }

  final class Abort<T> implements Result<T>, Recoverable {

    private final Throwable error;

    private Abort(Throwable error) {
      this.error = checkNonNull(error);
    }

    @Override
    public T value() {
      return sneakyThrow(error);
    }

    @Override
    public String toString() {
      return String.format("Error(%s)", error);
    }
  }

  final class Computation<T, R> implements Result<T> {

    private final Control<R> control;
    private final MetaCont<R, T> continuation;

    private Computation(Control<R> control, MetaCont<R, T> continuation) {
      this.control = checkNonNull(control);
      this.continuation = checkNonNull(continuation);
    }

    @Override
    public T value() {
      throw new UnsupportedOperationException();
    }

    public Result<T> apply() {
      return control.apply(continuation);
    }

    public Result<T> unwind(Throwable throwable) {
      return continuation.unwind(throwable);
    }

    @Override
    public String toString() {
      return String.format("Computation(%s, %s)", control, continuation);
    }
  }
}
