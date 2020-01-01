/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.util.function.Function;

public interface Higher2<F extends Kind, A, B> extends Higher1<Higher1<F, A>, B> {

  @Override
  default <R> R fix1(Function<? super Higher1<Higher1<F, A>, B>, ? extends R> function) {
    return Higher1.super.fix1(function);
  }

  default <R> R fix2(Function<? super Higher2<F, A, B>, ? extends R> function) {
    return function.apply(this);
  }

  default Higher2<F, A, B> kind2() {
    return this;
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind, A, B> Higher2<F, A, B> narrowK(Higher1<Higher1<F, A>, B> value) {
    return (Higher2<F, A, B>) value;
  }
}
