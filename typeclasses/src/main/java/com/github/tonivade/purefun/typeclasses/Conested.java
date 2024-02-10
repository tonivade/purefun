/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

@SuppressWarnings("unused")
public interface Conested<F extends Witness, A> extends Witness {

  @SuppressWarnings({"unchecked", "rawtypes"})
  static <F extends Witness, A, B> Kind<Conested<F, B>, A> conest(Kind<Kind<F, A>, ? extends B> counnested) {
    return (Kind) counnested;
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  static <F extends Witness, A, B> Kind<Kind<F, A>, B> counnest(Kind<Conested<F, B>, ? extends A> conested) {
    return (Kind) conested;
  }
}
