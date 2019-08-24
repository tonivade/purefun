/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.TypeClass;

@TypeClass
public interface Transformer<F extends Kind, G extends Kind> {

  <T> Higher1<G, T> apply(Higher1<F, T> from);

  default <B extends Kind> Transformer<B, G> compose(Transformer<B, F> before) {
    final Transformer<F, G> self = this;
    return new Transformer<B, G>() {
      @Override
      public <T> Higher1<G, T> apply(Higher1<B, T> from) {
        return self.apply(before.apply(from));
      }
    };
  }

  default <A extends Kind> Transformer<F, A> andThen(Transformer<G, A> after) {
    final Transformer<F, G> self = this;
    return new Transformer<F, A>() {
      @Override
      public <T> Higher1<A, T> apply(Higher1<F, T> from) {
        return after.apply(self.apply(from));
      }
    };
  }

  static <F extends Kind> Transformer<F, F> identity() {
    return new Transformer<F, F>() {
      @Override
      public <T> Higher1<F, T> apply(Higher1<F, T> from) {
        return from;
      }
    };
  }
}
