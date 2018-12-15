/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface Applicative<F extends Kind> extends Functor<F> {

  <T> Higher1<F, T> pure(T value);

  <T, R> Higher1<F, R> ap(Higher1<F, T> value, Higher1<F, Function1<T, R>> apply);

  @Override
  default <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map) {
    return ap(value, pure(map));
  }

  default <A, B, R> Higher1<F, R> map2(Higher1<F, A> fa, Higher1<F, B> fb, Function2<A, B, R> mapper) {
    return ap(fb, ap(fa, pure(mapper.curried())));
  }

  default <A, B, C, R> Higher1<F, R> map3(Higher1<F, A> fa, Higher1<F, B> fb, Higher1<F, C> fc,
      Function3<A, B, C, R> mapper) {
    return ap(fc, ap(fb, ap(fa, pure(mapper.curried()))));
  }

  default <A, B, C, D, R> Higher1<F, R> map4(Higher1<F, A> fa, Higher1<F, B> fb, Higher1<F, C> fc, Higher1<F, D> fd,
      Function4<A, B, C, D, R> mapper) {
    return ap(fd, ap(fc, ap(fb, ap(fa, pure(mapper.curried())))));
  }

  default <A, B, C, D, E, R> Higher1<F, R> map5(Higher1<F, A> fa, Higher1<F, B> fb, Higher1<F, C> fc, Higher1<F, D> fd,
      Higher1<F, E> fe, Function5<A, B, C, D, E, R> mapper) {
    return ap(fe, ap(fd, ap(fc, ap(fb, ap(fa, pure(mapper.curried()))))));
  }
}
