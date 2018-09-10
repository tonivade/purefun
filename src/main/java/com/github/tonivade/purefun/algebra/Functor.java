/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Witness;

public interface Functor<F extends Witness> {

  <T, R> Higher<F, R> map(Higher<F, T> value, Function1<T, R> map);

}