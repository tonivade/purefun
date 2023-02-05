/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

public interface SemigroupK<F extends Witness> {

  <T> Kind<F, T> combineK(Kind<F, ? extends T> t1, Kind<F, ? extends T> t2);
}
