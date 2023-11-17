/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface InjectK<F extends Witness, G extends Witness> {

  <T> Kind<G, T> inject(Kind<F, ? extends T> value);

  static <F extends Witness> InjectK<F, F> injectReflexive() {
    return Kind::narrowK;
  }
}
