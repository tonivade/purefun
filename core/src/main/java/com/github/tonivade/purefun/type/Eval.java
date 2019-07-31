/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Unit.unit;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;

@HigherKind
@FunctionalInterface
public interface Eval<T> {

  Eval<Boolean> TRUE = now(true);
  Eval<Boolean> FALSE = now(false);
  Eval<Unit> UNIT = now(unit());
  Eval<Integer> ZERO = now(0);
  Eval<Integer> ONE = now(1);

  T value();

  default <R> Eval<R> map(Function1<T, R> map) {
    return flatMap(value -> now(map.apply(value)));
  }

  default <R> Eval<R> flatMap(Function1<T, Eval<R>> map) {
    return later(() -> map.apply(value()).value());
  }

  static <T> Eval<T> now(T value) {
    return () -> value;
  }

  static <T> Eval<T> later(Producer<T> later) {
    return later.memoized()::get;
  }

  static <T> Eval<T> always(Producer<T> always) {
    return always::get;
  }

  static <T> Eval<T> defer(Producer<Eval<T>> eval) {
    return () -> eval.get().value();
  }
}
