/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.TypeClass;

@TypeClass
public interface Applicative<F extends Kind> extends Functor<F> {

  <T> Higher1<F, T> pure(T value);

  <T, R> Higher1<F, R> ap(Higher1<F, T> value, Higher1<F, Function1<T, R>> apply);

  @Override
  default <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map) {
    return ap(value, pure(map));
  }

  default <A, B> Higher1<F, Tuple2<A, B>> tuple(Higher1<F, A> fa, Higher1<F, B> fb) {
    return map2(fa, fb, Tuple2::of);
  }

  default <A, B, R> Higher1<F, R> map2(Higher1<F, A> fa, Higher1<F, B> fb, Function2<A, B, R> mapper) {
    return ap(fb, map(fa, mapper.curried()));
  }

  default <A, B, C, R> Higher1<F, R> map3(Higher1<F, A> fa, Higher1<F, B> fb, Higher1<F, C> fc,
      Function3<A, B, C, R> mapper) {
    return ap(fc, map2(fa, fb, (a, b) -> mapper.curried().apply(a).apply(b)));
  }

  default <A, B, C, D, R> Higher1<F, R> map4(Higher1<F, A> fa, Higher1<F, B> fb, Higher1<F, C> fc, Higher1<F, D> fd,
      Function4<A, B, C, D, R> mapper) {
    return ap(fd, map3(fa, fb, fc, (a, b, c) -> mapper.curried().apply(a).apply(b).apply(c)));
  }

  default <A, B, C, D, E, R> Higher1<F, R> map5(Higher1<F, A> fa, Higher1<F, B> fb, Higher1<F, C> fc, Higher1<F, D> fd,
      Higher1<F, E> fe, Function5<A, B, C, D, E, R> mapper) {
    return ap(fe, map4(fa, fb, fc, fd, (a, b, c, d) -> mapper.curried().apply(a).apply(b).apply(c).apply(d)));
  }

  default <A, B> Higher1<F, A> first(Higher1<F, A> fa, Higher1<F, B> fb) {
    return map2(fa, fb, (a, b) -> a);
  }

  default <A, B> Higher1<F, B> last(Higher1<F, A> fa, Higher1<F, B> fb) {
    return map2(fa, fb, (a, b) -> b);
  }

  static <F extends Kind, G extends Kind> Applicative<Nested<F, G>> compose(Applicative<F> f, Applicative<G> g) {
    return new ComposedApplicative<F, G>() {

      @Override
      public Applicative<F> f() { return f; }

      @Override
      public Applicative<G> g() { return g; }
    };
  }
}
