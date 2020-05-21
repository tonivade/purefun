/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.util.function.Function;

public interface Kind<F extends Witness, A> extends Witness {

  default <R> R fix(Function<? super Kind<F, A>, ? extends R> function) {
    return function.apply(this);
  }

  default Kind<F, A> kind() {
    return this;
  }
}
