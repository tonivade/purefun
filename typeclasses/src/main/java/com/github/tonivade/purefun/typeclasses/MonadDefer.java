/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Try;

public interface MonadDefer<F extends Kind> extends MonadThrow<F>, Bracket<F>, Defer<F> {

  default <A> Higher1<F, A> later(Producer<A> later) {
    return defer(() -> Try.of(later::get).fold(this::raiseError, this::pure));
  }
}
