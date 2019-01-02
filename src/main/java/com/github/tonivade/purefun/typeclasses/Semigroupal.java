/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple2;

public interface Semigroupal<F extends Kind> {

  <A, B> Higher1<F, Tuple2<A, B>> product(Higher1<F, A> a, Higher1<F, B> b);
}
