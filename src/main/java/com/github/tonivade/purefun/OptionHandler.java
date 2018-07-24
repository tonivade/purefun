/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Producer.unit;

@FunctionalInterface
public interface OptionHandler<T, R> extends Function1<T, Option<R>> {

  @Override
  default <V> OptionHandler<V, R> compose(Function1<V, T> before) {
    return value -> apply(before.apply(value));
  }

  default <V> OptionHandler<T, V> map(Function1<R, V> mapper) {
    return value -> apply(value).map(mapper::apply);
  }

  default <V> OptionHandler<T, V> flatMap(OptionHandler<R, V> mapper) {
    return value -> apply(value).flatMap(mapper::apply);
  }

  default <V> OptionHandler<T, V> flatten() {
    return value -> apply(value).flatten();
  }

  default OptionHandler<T, R> filter(Matcher<R> matcher) {
    return value -> apply(value).filter(matcher);
  }

  default Function1<T, R> orElse(R value) {
    return orElse(unit(value));
  }

  default Function1<T, R> orElse(Producer<R> producer) {
    return value -> apply(value).orElse(producer);
  }

  default OptionalHandler<T, R> toOptional() {
    return value -> apply(value).toOptional();
  }

  static <T, R> OptionHandler<T, R> of(Function1<T, Option<R>> reference) {
    return reference::apply;
  }

  static <T> OptionHandler<Option<T>, T> identity() {
    return Function1.<Option<T>>identity()::apply;
  }
}
