/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Nothing.nothing;

import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;

@FunctionalInterface
public interface Eval<T> extends FlatMap1<Eval.µ, T> {

  final class µ implements Kind {}

  Eval<Boolean> TRUE = now(true);
  Eval<Boolean> FALSE = now(false);
  Eval<Nothing> NOTHING = now(nothing());
  Eval<Integer> ZERO = now(0);
  Eval<Integer> ONE = now(1);

  T value();

  @Override
  default <R> Eval<R> map(Function1<T, R> map) {
    return flatMap(value -> now(map.apply(value)));
  }

  @Override
  default <R> Eval<R> flatMap(Function1<T, ? extends Higher1<Eval.µ, R>> map) {
    return later(() -> map.andThen(Eval::narrowK).apply(value()).value());
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

  static <T> Eval<T> narrowK(Higher1<Eval.µ, T> hkt) {
    return (Eval<T>) hkt;
  }
}
