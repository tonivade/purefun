/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;

public interface MonadReader<F extends Kind<F, ?>, R> extends Monad<F> {

  Kind<F, R> ask();

  default <A> Kind<F, A> reader(Function1<? super R, ? extends A> mapper) {
    return map(ask(), mapper);
  }
}
