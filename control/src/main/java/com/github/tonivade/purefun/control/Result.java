/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import static java.util.Objects.requireNonNull;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Sealed;

@Sealed
interface Result<T> {

  @SuppressWarnings("unchecked")
  static <T> T trampoline(Result<T> apply) {
    Result<T> result = apply;

    while (result.isComputation()) {
      Computation<T, ?> current = (Computation<T, ?>) result;

      try {
        result = current.apply();
      } catch (Throwable t) {
        result = current.unwind(t);
      }
    }

    return result.value();
  }

  T value();

  default boolean isComputation() {
    return false;
  }

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
      this.value = requireNonNull(value);
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
      this.error = requireNonNull(error);
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
      this.control = requireNonNull(control);
      this.continuation = requireNonNull(continuation);
    }

    @Override
    public boolean isComputation() {
      return true;
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
