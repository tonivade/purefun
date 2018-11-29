/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.handler;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.data.Sequence;

@FunctionalInterface
public interface SequenceHandler<T, R> extends Function1<T, Sequence<R>> {

  @Override
  default <V> SequenceHandler<V, R> compose(Function1<V, T> before) {
    return value -> apply(before.apply(value));
  }
  
  default <V> SequenceHandler<T, V> map(Function1<R, V> mapper) {
    return value -> apply(value).map(mapper::apply);
  }
  
  default <V> SequenceHandler<T, V> flatMap(SequenceHandler<R, V> mapper) {
    return value -> apply(value).flatMap(mapper::apply);
  }
  
  default <V> SequenceHandler<T, V> flatten() {
    return value -> apply(value).flatten();
  }
  
  default SequenceHandler<T, R> filter(Matcher1<R> matcher) {
    return value -> apply(value).filter(matcher::match);
  }

  default StreamHandler<T, R> toStreamHandler() {
    return value -> apply(value).stream();
  }

  static <T> SequenceHandler<Sequence<T>, T> identity() {
    return Function1.<Sequence<T>>identity()::apply;
  }
  
  static <T, R> SequenceHandler<T, R> of(Function1<T, ? extends Higher1<Sequence.µ, R>> reference) {
    return reference.andThen(Sequence::narrowK)::apply;
  }
}
