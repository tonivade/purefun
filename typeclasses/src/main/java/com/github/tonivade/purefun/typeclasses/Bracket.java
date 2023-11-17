/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface Bracket<F extends Witness, E> extends MonadError<F, E> {

  <A, B> Kind<F, B> bracket(
      Kind<F, ? extends A> acquire, 
      Function1<? super A, ? extends Kind<F, ? extends B>> use,
      Function1<? super A, ? extends Kind<F, Unit>> release);

  default <A, B> Kind<F, B> bracket(Kind<F, ? extends A> acquire, 
      Function1<? super A, ? extends Kind<F, ? extends B>> use, Consumer1<? super A> release) {
    return bracket(acquire, use, release.asFunction().andThen(this::pure));
  }

  default <A extends AutoCloseable, B> Kind<F, B>
      bracket(Kind<F, ? extends A> acquire, Function1<? super A, ? extends Kind<F, ? extends B>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }
}
