/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

public interface Alternative<F extends Kind<F, ?>> extends Applicative<F>, MonoidK<F> {

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> Alternative<Nested<F, G>> compose(Alternative<F> f, Alternative<G> g) {
    return new ComposedAlternative<>() {

      @Override
      public Alternative<F> f() { return f; }

      @Override
      public Alternative<G> g() { return g; }
    };
  }
}
