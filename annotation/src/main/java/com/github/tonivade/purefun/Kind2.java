/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.util.function.Function;

// TODO:
public interface Kind2<F, A, B> {

  default <R> R fix2(Function<? super Kind2<F, ? extends A, ? extends B>, ? extends R> fixer) {
    return fixer.apply(this);
  }

  default Kind2<F, A, B> kind2() {
    return this;
  }

  @SuppressWarnings("unchecked")
  static <F, A, B> Kind2<F, A, B> narrowK(Kind2<F, ? extends A, ? extends B> kind) {
    return (Kind2<F, A, B>) kind;
  }
}
