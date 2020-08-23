/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

public interface Conested<F extends Witness, A> extends Witness {

  @SuppressWarnings("unchecked")
  static <F extends Witness, A, B> Kind<Conested<F, B>, A> conest(Kind<Kind<F, A>, B> counnested) {
    return Kind.class.cast(counnested);
  }
  
  @SuppressWarnings("unchecked")
  static <F extends Witness, A, B> Kind<Kind<F, A>, B> counnest(Kind<Conested<F, B>, A> conested) {
    return Kind.class.cast(conested);
  }
}
