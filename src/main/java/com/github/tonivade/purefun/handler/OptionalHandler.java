/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.handler;

import static java.util.function.Function.identity;

import java.util.Optional;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Matcher;
import com.github.tonivade.purefun.Producer;

@FunctionalInterface
public interface OptionalHandler<T, R> extends Function1<T, Optional<R>> {
  
  @Override
  default <V> OptionalHandler<V, R> compose(Function1<V, T> before) {
    return value -> apply(before.apply(value));
  }
  
  default <V> OptionalHandler<T, V> map(Function1<R, V> mapper) {
    return value -> apply(value).map(mapper::apply);
  }
  
  default <V> OptionalHandler<T, V> flatMap(OptionalHandler<R, V> mapper) {
    return value -> apply(value).flatMap(mapper::apply);
  }
  
  @SuppressWarnings("unchecked")
  default <V> OptionalHandler<T, V> flatten() {
    return value -> ((Optional<Optional<V>>) apply(value)).flatMap(identity());
  }
  
  default OptionalHandler<T, R> filter(Matcher<R> matcher) {
    return value -> apply(value).filter(matcher::match);
  }
  
  default Function1<T, R> orElse(Producer<R> producer) {
    return value -> apply(value).orElseGet(producer::get);
  }
  
  static <T, R> OptionalHandler<T, R> of(Function1<T, Optional<R>> reference) {
    return reference::apply;
  }
}
