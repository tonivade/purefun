/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Kind<F extends Witness, A> extends Witness {

  default <R> R fix(Fixer<? super Kind<F, A>, ? extends R> fixer) {
    return fixer.apply(this);
  }

  default Kind<F, A> kind() {
    return this;
  }
  
  @SuppressWarnings("unchecked")
  static <F extends Witness, A> Kind<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Kind<F, A>) kind;
  }
}
