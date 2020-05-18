/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface Bracket<F extends Kind> {

  <A, B> Higher1<F, B> bracket(Higher1<F, A> acquire, Function1<A, ? extends Higher1<F, B>> use, Consumer1<A> release);

  default <A extends AutoCloseable, B> Higher1<F, B>
      bracket(Higher1<F, A> acquire,
              Function1<A, ? extends Higher1<F, B>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }
}
