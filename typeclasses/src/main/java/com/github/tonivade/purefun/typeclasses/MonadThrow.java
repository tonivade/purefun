/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.TypeClass;
import com.github.tonivade.purefun.type.Try;

@TypeClass
public interface MonadThrow<F extends Kind> extends MonadError<F, Throwable> {

  default <A> Higher1<F, A> fromTry(Try<A> value) {
    return fromEither(value.toEither());
  }
}
