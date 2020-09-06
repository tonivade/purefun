/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

public interface Bracket<F extends Witness, E> extends MonadError<F, E> {

  <A, B> Kind<F, B> bracket(Kind<F, A> acquire, Function1<A, ? extends Kind<F, B>> use, Consumer1<A> release);

  default <A extends AutoCloseable, B> Kind<F, B>
      bracket(Kind<F, A> acquire, Function1<A, ? extends Kind<F, B>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }
}
