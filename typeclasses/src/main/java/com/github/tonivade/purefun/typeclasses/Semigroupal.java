/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Tuple2;

public interface Semigroupal<F extends Witness> {

  <A, B> Kind<F, Tuple2<A, B>> product(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb);
}
