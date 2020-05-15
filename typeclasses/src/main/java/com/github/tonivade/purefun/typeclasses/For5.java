/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple5;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

public final class For5<F extends Kind, A, B, C, D, E> extends AbstractFor<F, D, E> {

  private final Producer<? extends Higher1<F, A>> value1;
  private final Function1<A, ? extends Higher1<F, B>> value2;
  private final Function1<B, ? extends Higher1<F, C>> value3;
  private final Function1<C, ? extends Higher1<F, D>> value4;

  protected For5(Monad<F> monad,
                 Producer<? extends Higher1<F, A>> value1,
                 Function1<A, ? extends Higher1<F, B>> value2,
                 Function1<B, ? extends Higher1<F, C>> value3,
                 Function1<C, ? extends Higher1<F, D>> value4,
                 Function1<D, ? extends Higher1<F, E>> value5) {
    super(monad, value5);
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
    this.value3 = checkNonNull(value3);
    this.value4 = checkNonNull(value4);
  }

  public Higher1<F, Tuple5<A, B, C, D, E>> tuple() {
    return apply(Tuple5::of);
  }

  public <R> Higher1<F, R> apply(Function5<A, B, C, D, E, R> combine) {
    Higher1<F, A> fa = value1.get();
    Higher1<F, B> fb = monad.flatMap(fa, value2);
    Higher1<F, C> fc = monad.flatMap(fb, value3);
    Higher1<F, D> fd = monad.flatMap(fc, value4);
    Higher1<F, E> fe = monad.flatMap(fd, value);
    return monad.map5(fa, fb, fc, fd, fe, combine);
  }

  public <R> Higher1<F, R> yield(Function5<A, B, C, D, E, R> combine) {
    return monad.flatMap(value1.get(),
        a -> monad.flatMap(value2.apply(a),
            b -> monad.flatMap(value3.apply(b),
                c -> monad.flatMap(value4.apply(c),
                    d -> monad.map(value.apply(d),
                        e -> combine.apply(a, b, c, d, e))))));
  }

  @Override
  public Higher1<F, E> run() {
    return yield((a, b, c, d, e) -> e);
  }
}
