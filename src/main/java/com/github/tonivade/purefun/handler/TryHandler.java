/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.handler;

import static com.github.tonivade.purefun.Producer.unit;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface TryHandler<T, R> extends Function1<T, Try<R>> {

  @Override
  default <V> TryHandler<V, R> compose(Function1<V, T> before) {
    return value -> apply(before.apply(value));
  }

  default <V> TryHandler<T, V> map(Function1<R, V> mapper) {
    return value -> apply(value).map(mapper::apply);
  }

  default <V> TryHandler<T, V> flatMap(TryHandler<R, V> mapper) {
    return value -> apply(value).flatMap(mapper::apply);
  }

  default <V> TryHandler<T, V> flatten() {
    return value -> apply(value).flatten();
  }

  default TryHandler<T, R> recover(Function1<Throwable, R> mapper) {
    return value -> apply(value).recover(mapper);
  }

  default TryHandler<T, R> filter(Matcher1<R> matcher) {
    return value -> apply(value).filter(matcher);
  }

  default Function1<T, R> orElse(R value) {
    return orElse(unit(value));
  }

  default Function1<T, R> orElse(Producer<R> producer) {
    return value -> apply(value).orElse(producer);
  }

  default OptionHandler<T, R> toOption() {
    return value -> apply(value).toOption();
  }

  default EitherHandler<T, Throwable, R> toEither() {
    return value -> apply(value).toEither();
  }

  static <T> TryHandler<Try<T>, T> identity() {
    return Function1.<Try<T>>identity()::apply;
  }

  static <T, R> TryHandler<T, R> of(Function1<T, ? extends Higher1<Try.µ, R>> reference) {
    return reference.andThen(Try::narrowK)::apply;
  }
}
