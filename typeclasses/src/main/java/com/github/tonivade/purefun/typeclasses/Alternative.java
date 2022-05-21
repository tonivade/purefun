/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Witness;

public interface Alternative<F extends Witness> extends Applicative<F>, MonoidK<F> {

  static <F extends Witness, G extends Witness> Alternative<Nested<F, G>> compose(Alternative<F> f, Alternative<G> g) {
    return new ComposedAlternative<>() {

      @Override
      public Alternative<F> f() { return f; }

      @Override
      public Alternative<G> g() { return g; }
    };
  }
}
