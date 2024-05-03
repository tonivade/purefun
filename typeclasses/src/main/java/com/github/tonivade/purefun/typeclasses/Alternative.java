/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;



public interface Alternative<F> extends Applicative<F>, MonoidK<F> {

  static <F, G> Alternative<Nested<F, G>> compose(Alternative<F> f, Alternative<G> g) {
    return new ComposedAlternative<>() {

      @Override
      public Alternative<F> f() { return f; }

      @Override
      public Alternative<G> g() { return g; }
    };
  }
}
