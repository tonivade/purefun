/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.handler;

import static com.github.tonivade.purefun.Producer.unit;
import static com.github.tonivade.purefun.type.OptionKind.narrowK;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Matcher;
import com.github.tonivade.purefun.Monad;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionKind;

@FunctionalInterface
public interface OptionHandler<T, R> extends Function1<T, Monad<OptionKind.µ, R>> {

  default Option<R> applyK(T value) {
    return narrowK(apply(value));
  }

  @Override
  default <V> OptionHandler<V, R> compose(Function1<V, T> before) {
    return value -> applyK(before.apply(value));
  }

  default <V> OptionHandler<T, V> map(Function1<R, V> mapper) {
    return value -> applyK(value).map(mapper::apply);
  }

  default <V> OptionHandler<T, V> flatMap(OptionHandler<R, V> mapper) {
    return value -> applyK(value).flatMap(mapper::apply);
  }

  default <V> OptionHandler<T, V> flatten() {
    return value -> applyK(value).flatten();
  }

  default OptionHandler<T, R> filter(Matcher<R> matcher) {
    return value -> applyK(value).filter(matcher);
  }

  default Function1<T, R> orElse(R value) {
    return orElse(unit(value));
  }

  default Function1<T, R> orElse(Producer<R> producer) {
    return value -> applyK(value).orElse(producer);
  }

  default OptionalHandler<T, R> toOptional() {
    return value -> applyK(value).toOptional();
  }

  static <T, R> OptionHandler<T, R> of(Function1<T, Option<R>> reference) {
    return reference::apply;
  }

  static <T> OptionHandler<Option<T>, T> identity() {
    return Function1.<Option<T>>identity()::apply;
  }
}
