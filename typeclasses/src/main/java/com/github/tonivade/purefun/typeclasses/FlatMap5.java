/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Function5.fifth;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function5;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple5;

public final class FlatMap5<F extends Witness, A, B, C, D, E> extends AbstractFlatMap<F, D, E> {

  private final Producer<? extends Kind<F, ? extends A>> value1;
  private final Function1<? super A, ? extends Kind<F, ? extends B>> value2;
  private final Function1<? super B, ? extends Kind<F, ? extends C>> value3;
  private final Function1<? super C, ? extends Kind<F, ? extends D>> value4;

  FlatMap5(Monad<F> monad,
                 Producer<? extends Kind<F, ? extends A>> value1,
                 Function1<? super A, ? extends Kind<F, ? extends B>> value2,
                 Function1<? super B, ? extends Kind<F, ? extends C>> value3,
                 Function1<? super C, ? extends Kind<F, ? extends D>> value4,
                 Function1<? super D, ? extends Kind<F, ? extends E>> value5) {
    super(monad, value5);
    this.value1 = checkNonNull(value1);
    this.value2 = checkNonNull(value2);
    this.value3 = checkNonNull(value3);
    this.value4 = checkNonNull(value4);
  }

  public Kind<F, Tuple5<A, B, C, D, E>> tuple() {
    return apply(Tuple5::of);
  }

  public <R> Kind<F, R> apply(Function5<A, B, C, D, E, R> combine) {
    return monad.flatMap(value1.get(),
        a -> monad.flatMap(value2.apply(a),
            b -> monad.flatMap(value3.apply(b),
                c -> monad.flatMap(value4.apply(c),
                    d -> monad.map(value.apply(d),
                        e -> combine.apply(a, b, c, d, e))))));
  }

  @Override
  public Kind<F, E> run() {
    return apply(fifth());
  }
}
