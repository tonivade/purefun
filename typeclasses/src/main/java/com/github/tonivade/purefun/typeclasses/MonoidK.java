/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

public interface MonoidK<F extends Kind<F, ?>> extends SemigroupK<F> {

  <T> Kind<F, T> zero();
}