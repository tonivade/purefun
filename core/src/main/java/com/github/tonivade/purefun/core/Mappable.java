/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import com.github.tonivade.purefun.Kind;


public interface Mappable<F, A> extends Kind<F, A> {
  
  <R> Mappable<F, R> map(Function1<? super A, ? extends R> mapper);
  
  @SuppressWarnings("unchecked")
  static <F, A> Mappable<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Mappable<F, A>) kind;
  }
}
