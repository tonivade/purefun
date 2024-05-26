/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Kind2;

@SuppressWarnings("unused")
public interface Conested<F, A> {

  @SuppressWarnings({"unchecked", "rawtypes"})
  static <F, A, B> Kind<Conested<F, B>, A> conest(Kind2<F, A, ? extends B> counnested) {
    return (Kind) counnested;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  static <F, A, B> Kind2<F, A, B> counnest(Kind<Conested<F, B>, ? extends A> conested) {
    return (Kind2) conested;
  }
}
