/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.TypeClass;

@TypeClass
public interface Alternative<F extends Kind> extends Applicative<F>, MonoidK<F> {

  static <F extends Kind, G extends Kind> Alternative<Nested<F, G>> compose(Alternative<F> f, Alternative<G> g) {
    return new ComposedAlternative<F, G>() {

      @Override
      public Alternative<F> f() { return f; }

      @Override
      public Alternative<G> g() { return g; }
    };
  }
}
