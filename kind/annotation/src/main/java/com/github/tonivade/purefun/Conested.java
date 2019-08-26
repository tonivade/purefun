/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Conested<F extends Kind, A> extends Higher1<F, A> {

  @SuppressWarnings("unchecked")
  static <F extends Kind, A, B> Higher1<Conested<F, B>, A> conest(Higher1<Higher1<F, A>, B> counested) {
    return (Higher1<Conested<F, B>, A>) Higher1.class.cast(counested);
  }
  
  @SuppressWarnings("unchecked")
  static <F extends Kind, A, B> Higher1<Higher1<F, A>, B> counnest(Higher1<Conested<F, B>, A> conested) {
    return (Higher1<Higher1<F, A>, B>) Higher1.class.cast(conested);
  }
}
