/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

public interface FunctionK<F extends Kind<F, ?>, G extends Kind<G, ?>> {

  <T> Kind<G, T> apply(Kind<F, ? extends T> from);

  default <B extends Kind<B, ?>> FunctionK<B, G> compose(FunctionK<B, F> before) {
    final FunctionK<F, G> self = this;
    return new FunctionK<>() {
      @Override
      public <T> Kind<G, T> apply(Kind<B, ? extends T> from) {
        return self.apply(before.apply(from));
      }
    };
  }

  default <A extends Kind<A, ?>> FunctionK<F, A> andThen(FunctionK<G, A> after) {
    final FunctionK<F, G> self = this;
    return new FunctionK<>() {
      @Override
      public <T> Kind<A, T> apply(Kind<F, ? extends T> from) {
        return after.apply(self.apply(from));
      }
    };
  }

  static <F extends Kind<F, ?>> FunctionK<F, F> identity() {
    return Kind::narrowK;
  }
}
