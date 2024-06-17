/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Kind<F extends Kind<F, ?>, A> {

  @SuppressWarnings("unchecked")
  default <R> R fix() {
    return (R) this;
  }

  default Kind<F, A> kind() {
    return this;
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>, A> Kind<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Kind<F, A>) kind;
  }
}
