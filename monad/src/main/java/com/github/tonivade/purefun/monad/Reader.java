/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;

@HigherKind
@FunctionalInterface
public interface Reader<R, A> extends ReaderOf<R, A> {

  A eval(R reader);

  default <B> Reader<R, B> map(Function1<? super A, ? extends B> mapper) {
    return reader -> mapper.apply(eval(reader));
  }

  default <B> Reader<R, B> flatMap(Function1<? super A, ? extends Reader<R, ? extends B>> mapper) {
    return reader -> mapper.apply(eval(reader)).eval(reader);
  }

  default <B> Reader<R, B> andThen(Reader<R, ? extends B> next) {
    return flatMap(ignore -> next);
  }

  static <R> Reader<R, R> env() {
    return reader -> reader;
  }

  static <R, A> Reader<R, A> pure(A value) {
    return reader -> value;
  }

  static <R, A> Reader<R, A> reader(Function1<? super R, ? extends A> run) {
    return run::apply;
  }
}
