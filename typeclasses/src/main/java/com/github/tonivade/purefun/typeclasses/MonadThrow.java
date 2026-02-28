/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.type.Try;

public interface MonadThrow<F extends Kind<F, ?>> extends MonadError<F, Throwable> {

  default <A> Kind<F, A> fromTry(Try<? extends A> value) {
    return fromEither(value.toEither());
  }
}
