/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.TypeClass;

@TypeClass
public interface InjectK<F extends Kind, G extends Kind> {

  <T> Higher1<G, T> inject(Higher1<F, T> value);

  static <F extends Kind> InjectK<F, F> injectReflexive() {
    return new InjectK<F, F>() {
      @Override
      public <T> Higher1<F, T> inject(Higher1<F, T> value) {
        return value;
      }
    };
  }
}
