/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface Applicative<F extends Kind> extends Functor<F> {
  
  <T> Higher1<F, T> pure(T value);
  
  <T, R> Higher1<F, R> ap(Higher1<F, T> value, Higher1<F, Function1<T, R>> apply);
  
  @Override
  default <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map) {
    return ap(value, pure(map));
  }
}
