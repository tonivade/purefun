/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Nested<F extends Kind, G extends Kind> extends Kind {

  @SuppressWarnings("unchecked")
  static <F extends Kind, G extends Kind, A> Higher1<Nested<F, G>, A> nest(Higher1<F, Higher1<G, A>> unnested) {
    return (Higher1<Nested<F, G>, A>) unnested;
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind, G extends Kind, A> Higher1<F, Higher1<G, A>> unnest(Higher1<Nested<F, G>, A> nested) {
    return (Higher1<F, Higher1<G, A>>) nested;
  }
}
