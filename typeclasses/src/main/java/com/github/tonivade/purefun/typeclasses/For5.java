/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple5;

public final class For5<F extends Kind, A, B, C, D, E> extends AbstractFor<F, E> {

  private final Higher1<F, A> value1;
  private final Higher1<F, B> value2;
  private final Higher1<F, C> value3;
  private final Higher1<F, D> value4;

  protected For5(Monad<F> monad,
                 Higher1<F, A> value1,
                 Higher1<F, B> value2,
                 Higher1<F, C> value3,
                 Higher1<F, D> value4,
                 Higher1<F, E> value5) {
    super(monad, value5);
    this.value1 = requireNonNull(value1);
    this.value2 = requireNonNull(value2);
    this.value3 = requireNonNull(value3);
    this.value4 = requireNonNull(value4);
  }

  public Higher1<F, Tuple5<A, B, C, D, E>> tuple() {
    return apply(Tuple5::of);
  }

  public <R> Higher1<F, R> apply(Function5<A, B, C, D, E, R> combine) {
    return monad.map5(value1, value2, value3, value4, value, combine);
  }
}
