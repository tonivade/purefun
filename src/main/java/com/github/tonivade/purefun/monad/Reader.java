/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Monad2;
import com.github.tonivade.purefun.Kind;

@FunctionalInterface
public interface Reader<R, A> extends Monad2<Reader.µ, R, A> {

  final class µ implements Kind {}

  A eval(R reader);

  @Override
  default <B> Reader<R, B> map(Function1<A, B> mapper) {
    return reader -> mapper.apply(eval(reader));
  }

  @Override
  default <B> Reader<R, B> flatMap(Function1<A, ? extends Higher2<Reader.µ, R, B>> mapper) {
    return reader -> mapper.andThen(Reader::narrowK).apply(eval(reader)).eval(reader);
  }

  static <R, A> Reader<R, A> pure(A value) {
    return reader -> value;
  }

  static <R, A> Reader<R, A> reader(Function1<R, A> run) {
    return run::apply;
  }

  static <R, A> Reader<R, A> narrowK(Higher2<Reader.µ, R, A> hkt) {
    return (Reader<R, A>) hkt;
  }

  static <R, A> Reader<R, A> narrowK(Higher1<Higher1<Reader.µ, R>, A> hkt) {
    return (Reader<R, A>) hkt;
  }
}
