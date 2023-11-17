/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface Mappable<F extends Witness, A> extends Kind<F, A> {
  
  <R> Mappable<F, R> map(Function1<? super A, ? extends R> mapper);
  
  @SuppressWarnings("unchecked")
  static <F extends Witness, A> Mappable<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Mappable<F, A>) kind;
  }
}
