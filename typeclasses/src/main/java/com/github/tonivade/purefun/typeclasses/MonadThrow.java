/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Try;

public interface MonadThrow<F extends Witness> extends MonadError<F, Throwable> {

  default <A> Kind<F, A> fromTry(Try<? extends A> value) {
    return fromEither(value.toEither());
  }
}
