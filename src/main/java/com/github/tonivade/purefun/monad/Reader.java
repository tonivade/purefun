/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Functor;

@FunctionalInterface
public interface Reader<R, A> extends Functor<A> {

  A run(R reader);

  @Override
  default <B> Reader<R, B> map(Function1<A, B> mapper) {
    return reader -> mapper.apply(run(reader));
  }

  default <B> Reader<R, B> flatMap(Function1<A, Reader<R, B>> mapper) {
    return reader -> mapper.apply(run(reader)).run(reader);
  }

  static <R, A> Reader<R, A> unit(A value) {
    return reader -> value;
  }

  static <R, A> Reader<R, A> reader(Function1<R, A> run) {
    return run::apply;
  }
}
