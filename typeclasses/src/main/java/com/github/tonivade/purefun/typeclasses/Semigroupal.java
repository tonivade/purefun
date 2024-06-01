/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Tuple2;

public interface Semigroupal<F extends Kind<F, ?>> {

  <A, B> Kind<F, Tuple2<A, B>> product(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb);
}
