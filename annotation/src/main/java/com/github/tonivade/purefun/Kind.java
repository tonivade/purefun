/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Kind<F, A> {

  default <R> R fix(Fixer<? super Kind<F, A>, ? extends R> fixer) {
    return fixer.apply(this);
  }

  default Kind<F, A> kind() {
    return this;
  }

  @SuppressWarnings("unchecked")
  static <F, A> Kind<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Kind<F, A>) kind;
  }
}
