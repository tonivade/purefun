/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Tuple2;

public interface Applicative<F extends Witness> extends Functor<F> {

  <T> Kind<F, T> pure(T value);

  <T, R> Kind<F, R> ap(Kind<F, T> value, Kind<F, Function1<T, R>> apply);

  @Override
  default <T, R> Kind<F, R> map(Kind<F, T> value, Function1<T, R> map) {
    return ap(value, pure(map));
  }

  default <A, B> Kind<F, Tuple2<A, B>> tuple(Kind<F, A> fa, Kind<F, B> fb) {
    return mapN(fa, fb, Tuple2::of);
  }

  default <A, B, R> Kind<F, R> mapN(Kind<F, A> fa, Kind<F, B> fb, Function2<A, B, R> mapper) {
    return ap(fb, map(fa, mapper.curried()));
  }

  default <A, B, C, R> Kind<F, R> mapN(Kind<F, A> fa, Kind<F, B> fb, Kind<F, C> fc,
      Function3<A, B, C, R> mapper) {
    return ap(fc, mapN(fa, fb, (a, b) -> mapper.curried().apply(a).apply(b)));
  }

  default <A, B, C, D, R> Kind<F, R> mapN(Kind<F, A> fa, Kind<F, B> fb, Kind<F, C> fc, Kind<F, D> fd,
      Function4<A, B, C, D, R> mapper) {
    return ap(fd, mapN(fa, fb, fc, (a, b, c) -> mapper.curried().apply(a).apply(b).apply(c)));
  }

  default <A, B, C, D, E, R> Kind<F, R> mapN(Kind<F, A> fa, Kind<F, B> fb, Kind<F, C> fc, Kind<F, D> fd,
      Kind<F, E> fe, Function5<A, B, C, D, E, R> mapper) {
    return ap(fe, mapN(fa, fb, fc, fd, (a, b, c, d) -> mapper.curried().apply(a).apply(b).apply(c).apply(d)));
  }

  default <A, B> Kind<F, A> first(Kind<F, A> fa, Kind<F, B> fb) {
    return mapN(fa, fb, (a, b) -> a);
  }

  default <A, B> Kind<F, B> last(Kind<F, A> fa, Kind<F, B> fb) {
    return mapN(fa, fb, (a, b) -> b);
  }

  static <F extends Witness, G extends Witness> Applicative<Nested<F, G>> compose(Applicative<F> f, Applicative<G> g) {
    return new ComposedApplicative<F, G>() {

      @Override
      public Applicative<F> f() { return f; }

      @Override
      public Applicative<G> g() { return g; }
    };
  }
}
