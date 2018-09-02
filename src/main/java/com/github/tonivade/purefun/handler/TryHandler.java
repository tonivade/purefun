/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.handler;

import static com.github.tonivade.purefun.Producer.unit;
import static com.github.tonivade.purefun.type.TryKind.narrowK;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Matcher;
import com.github.tonivade.purefun.Monad;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryKind;

@FunctionalInterface
public interface TryHandler<T, R> extends Function1<T, Monad<TryKind.µ, R>> {

  default Try<R> applyK(T value) {
    return narrowK(apply(value));
  }

  @Override
  default <V> TryHandler<V, R> compose(Function1<V, T> before) {
    return value -> applyK(before.apply(value));
  }

  default <V> TryHandler<T, V> map(Function1<R, V> mapper) {
    return value -> applyK(value).map(mapper::apply);
  }

  default <V> TryHandler<T, V> flatMap(TryHandler<R, V> mapper) {
    return value -> applyK(value).flatMap(mapper::apply);
  }

  default <V> TryHandler<T, V> flatten() {
    return value -> applyK(value).flatten();
  }

  default TryHandler<T, R> recover(Function1<Throwable, R> mapper) {
    return value -> applyK(value).recover(mapper);
  }

  default TryHandler<T, R> filter(Matcher<R> matcher) {
    return value -> applyK(value).filter(matcher);
  }

  default Function1<T, R> orElse(R value) {
    return orElse(unit(value));
  }

  default Function1<T, R> orElse(Producer<R> producer) {
    return value -> applyK(value).orElse(producer);
  }

  default OptionHandler<T, R> toOption() {
    return value -> applyK(value).toOption();
  }

  default EitherHandler<T, Throwable, R> toEither() {
    return value -> applyK(value).toEither();
  }

  static <T> TryHandler<Try<T>, T> identity() {
    return Function1.<Try<T>>identity()::apply;
  }

  static <T, R> TryHandler<T, R> of(Function1<T, Try<R>> reference) {
    return reference::apply;
  }
}
