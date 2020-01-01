/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.util.function.Function;

public interface Higher3<F extends Kind, A, B, C> extends Higher2<Higher1<F, A>, B, C>, Higher1<Higher1<Higher1<F, A>, B>, C> {

  @Override
  default <R> R fix1(Function<? super Higher1<Higher1<Higher1<F, A>, B>, C>, ? extends R> function) {
    return Higher2.super.fix1(function);
  }

  @Override
  default <R> R fix2(Function<? super Higher2<Higher1<F, A>, B, C>, ? extends R> function) {
    return Higher2.super.fix2(function);
  }

  default <R> R fix3(Function<? super Higher3<F, A, B, C>, ? extends R> function) {
    return function.apply(this);
  }

  default Higher3<F, A, B, C> kind3() {
    return this;
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind, A, B, C> Higher3<F, A, B, C> narrowK(Higher1<Higher1<Higher1<F, A>, B>, C> value) {
    return (Higher3<F, A, B, C>) value;
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind, A, B, C> Higher3<F, A, B, C> narrowK(Higher2<Higher1<F, A>, B, C> value) {
    return (Higher3<F, A, B, C>) value;
  }
}
