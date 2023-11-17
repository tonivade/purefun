/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface Semigroupal<F extends Witness> {

  <A, B> Kind<F, Tuple2<A, B>> product(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb);
}
