/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface Applicative<F extends Witness> extends Functor<F> {

  <T> Kind<F, T> pure(T value);

  <T, R> Kind<F, R> ap(Kind<F, ? extends T> value, Kind<F, ? extends Function1<? super T, ? extends R>> apply);

  @Override
  default <T, R> Kind<F, R> map(Kind<F, ? extends T> value, Function1<? super T, ? extends R> map) {
    return ap(value, pure(map));
  }

  default <A, B> Kind<F, Tuple2<A, B>> tuple(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb) {
    return mapN(fa, fb, Tuple2::of);
  }

  default <A, B, R> Kind<F, R> mapN(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb, Function2<? super A, ? super B, ? extends R> mapper) {
    return ap(fb, map(fa, mapper.curried()));
  }

  default <A, B, C, R> Kind<F, R> mapN(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb, Kind<F, ? extends C> fc,
      Function3<? super A, ? super B, ? super C, ? extends R> mapper) {
    return ap(fc, mapN(fa, fb, (a, b) -> mapper.curried().apply(a).apply(b)));
  }

  default <A, B, C, D, R> Kind<F, R> mapN(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb, Kind<F, ? extends C> fc, Kind<F, ? extends D> fd,
      Function4<? super A, ? super B, ? super C, ? super D, ? extends R> mapper) {
    return ap(fd, mapN(fa, fb, fc, (a, b, c) -> mapper.curried().apply(a).apply(b).apply(c)));
  }

  default <A, B, C, D, E, R> Kind<F, R> mapN(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb, Kind<F, ? extends C> fc, Kind<F, ? extends D> fd,
      Kind<F, ? extends E> fe, Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> mapper) {
    return ap(fe, mapN(fa, fb, fc, fd, (a, b, c, d) -> mapper.curried().apply(a).apply(b).apply(c).apply(d)));
  }

  default <A, B> Kind<F, A> first(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb) {
    return mapN(fa, fb, Function2.first());
  }

  default <A, B> Kind<F, B> last(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb) {
    return mapN(fa, fb, Function2.second());
  }

  static <F extends Witness, G extends Witness> Applicative<Nested<F, G>> compose(Applicative<F> f, Applicative<G> g) {
    return new ComposedApplicative<>() {

      @Override
      public Applicative<F> f() { return f; }

      @Override
      public Applicative<G> g() { return g; }
    };
  }
}
