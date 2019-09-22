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

/**
 * <p>This is a monad that allows to control the evaluation of a computation or a value.</p>
 * <p>There are 3 basic strategies:</p>
 * <ul>
 *   <li>Eval.now(): evaluated immediately</li>
 *   <li>Eval.later(): the computation is evaluated later, but only the first time, the result is memoized.</li>
 *   <li>Eval.always(): the computation is evaluated later, but is always executed.</li>
 * </ul>
 * <p><strong>Warning:</strong> Not stack safe</p>
 * @param <T> result of the computation
 */
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
