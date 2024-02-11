/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;

@HigherKind
@FunctionalInterface
public non-sealed interface Reader<R, A> extends ReaderOf<R, A>, Bindable<Kind<Reader_, R>, A> {

  A eval(R reader);

  @Override
  default <B> Reader<R, B> map(Function1<? super A, ? extends B> mapper) {
    return reader -> mapper.apply(eval(reader));
  }

  @Override
  default <B> Reader<R, B> flatMap(Function1<? super A, ? extends Kind<Kind<Reader_, R>, ? extends B>> mapper) {
    return reader -> mapper.andThen(ReaderOf::<R, B>narrowK).apply(eval(reader)).eval(reader);
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
