/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

public interface FunctionK<F, G> {

  <T> Kind<G, T> apply(Kind<F, ? extends T> from);

  default <B> FunctionK<B, G> compose(FunctionK<B, F> before) {
    final FunctionK<F, G> self = this;
    return new FunctionK<>() {
      @Override
      public <T> Kind<G, T> apply(Kind<B, ? extends T> from) {
        return self.apply(before.apply(from));
      }
    };
  }

  default <A> FunctionK<F, A> andThen(FunctionK<G, A> after) {
    final FunctionK<F, G> self = this;
    return new FunctionK<>() {
      @Override
      public <T> Kind<A, T> apply(Kind<F, ? extends T> from) {
        return after.apply(self.apply(from));
      }
    };
  }

  static <F> FunctionK<F, F> identity() {
    return Kind::narrowK;
  }
}
