/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;


public interface SemigroupK<F> {

  <T> Kind<F, T> combineK(Kind<F, ? extends T> t1, Kind<F, ? extends T> t2);
}
