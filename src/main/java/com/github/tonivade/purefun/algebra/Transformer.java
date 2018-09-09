/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.IdKind;
import com.github.tonivade.purefun.type.IdKind.µ;

public interface Transformer<F extends Witness, T extends Witness> {

  <X> Higher<T, X> apply(Higher<F, X> from);

  default <B extends Witness> Transformer<B, T> compose(Transformer<B, F> before) {
    return new Transformer<B, T>() {
      @Override
      public <X> Higher<T, X> apply(Higher<B, X> from) {
        return Transformer.this.apply(before.apply(from));
      }
    };
  }

  default <A extends Witness> Transformer<F, A> andThen(Transformer<T, A> after) {
    return new Transformer<F, A>() {
      @Override
      public <X> Higher<A, X> apply(Higher<F, X> from) {
        return after.apply(Transformer.this.apply(from));
      }
    };
  }

  static Transformer<IdKind.µ, IdKind.µ> id() {
    return new Transformer<IdKind.µ, IdKind.µ>() {
      @Override
      public <X> Id<X> apply(Higher<µ, X> from) {
        return IdKind.narrowK(from);
      }
    };
  }
}
