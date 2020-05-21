/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

public interface InjectK<F extends Witness, G extends Witness> {

  <T> Kind<G, T> inject(Kind<F, T> value);

  static <F extends Witness> InjectK<F, F> injectReflexive() {
    return new InjectK<F, F>() {
      @Override
      public <T> Kind<F, T> inject(Kind<F, T> value) {
        return value;
      }
    };
  }
}
