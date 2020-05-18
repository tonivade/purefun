/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface FunctionK<F extends Kind, G extends Kind> {

  <T> Higher1<G, T> apply(Higher1<F, T> from);

  default <B extends Kind> FunctionK<B, G> compose(FunctionK<B, F> before) {
    final FunctionK<F, G> self = this;
    return new FunctionK<B, G>() {
      @Override
      public <T> Higher1<G, T> apply(Higher1<B, T> from) {
        return self.apply(before.apply(from));
      }
    };
  }

  default <A extends Kind> FunctionK<F, A> andThen(FunctionK<G, A> after) {
    final FunctionK<F, G> self = this;
    return new FunctionK<F, A>() {
      @Override
      public <T> Higher1<A, T> apply(Higher1<F, T> from) {
        return after.apply(self.apply(from));
      }
    };
  }

  static <F extends Kind> FunctionK<F, F> identity() {
    return new FunctionK<F, F>() {
      @Override
      public <T> Higher1<F, T> apply(Higher1<F, T> from) {
        return from;
      }
    };
  }
}
