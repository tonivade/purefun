/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

public interface MonadReader<F extends Witness, R> extends Monad<F> {

  Kind<F, R> ask();

  default <A> Kind<F, A> reader(Function1<R, A> mapper) {
    return map(ask(), mapper);
  }
}
