/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

public interface FunctionK<F extends Witness, G extends Witness> {

  <T> Kind<G, T> apply(Kind<F, ? extends T> from);

  default <B extends Witness> FunctionK<B, G> compose(FunctionK<B, F> before) {
    final FunctionK<F, G> self = this;
    return new FunctionK<>() {
      @Override
      public <T> Kind<G, T> apply(Kind<B, ? extends T> from) {
        return self.apply(before.apply(from));
      }
    };
  }

  default <A extends Witness> FunctionK<F, A> andThen(FunctionK<G, A> after) {
    final FunctionK<F, G> self = this;
    return new FunctionK<>() {
      @Override
      public <T> Kind<A, T> apply(Kind<F, ? extends T> from) {
        return after.apply(self.apply(from));
      }
    };
  }

  static <F extends Witness> FunctionK<F, F> identity() {
    return new FunctionK<F, F>() {
      @Override
      public <T> Kind<F, T> apply(Kind<F, ? extends T> from) {
        return Kind.narrowK(from);
      }
    };
  }
}
