/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Higher1<F extends Kind, A> extends Kind {

  default <R> R fix1(Function1<? super Higher1<F, A>, ? extends R> function) {
    return function.apply(this);
  }
}
