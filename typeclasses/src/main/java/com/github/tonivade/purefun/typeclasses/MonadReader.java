/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface MonadReader<F extends Kind, R> extends Monad<F> {

  Higher1<F, R> ask();

  default <A> Higher1<F, A> reader(Function1<R, A> mapper) {
    return map(ask(), mapper);
  }
}
