/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Function3;
import com.github.tonivade.purefun.core.Function4;
import com.github.tonivade.purefun.core.Function5;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Tuple3;
import com.github.tonivade.purefun.core.Tuple4;
import com.github.tonivade.purefun.core.Tuple5;

public interface Applicative<F extends Witness> extends Functor<F> {

  <T> Kind<F, T> pure(T value);

  <T, R> Kind<F, R> ap(Kind<F, ? extends T> value, Kind<F, ? extends Function1<? super T, ? extends R>> apply);

  @Override
  default <T, R> Kind<F, R> map(Kind<F, ? extends T> value, Function1<? super T, ? extends R> map) {
    return ap(value, pure(map));
  }

  default <A, B> Kind<F, Tuple2<A, B>> tuple(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb) {
    return mapN(fa, fb).apply(Tuple2::of);
  }

  default <A, B, C> Kind<F, Tuple3<A, B, C>> tuple(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc) {
    return mapN(fa, fb, fc).apply(Tuple3::of);
  }

  default <A, B, C, D> Kind<F, Tuple4<A, B, C, D>> tuple(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc,
      Kind<F, ? extends D> fd) {
    return mapN(fa, fb, fc, fd).apply(Tuple4::of);
  }

  default <A, B, C, D, E> Kind<F, Tuple5<A, B, C, D, E>> tuple(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc,
      Kind<F, ? extends D> fd,
      Kind<F, ? extends E> fe) {
    return mapN(fa, fb, fc, fd, fe).apply(Tuple5::of);
  }

  default <A, B, R> Kind<F, R> mapN(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Function2<? super A, ? super B, ? extends R> mapper) {
    return ap(fb, map(fa, mapper.curried()));
  }

  default <A, B> Map2<F, A, B> mapN(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb) {
    return new Map2<>(this, fa, fb);
  }

  default <A, B, C, R> Kind<F, R> mapN(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc,
      Function3<? super A, ? super B, ? super C, ? extends R> mapper) {
    return ap(fc, mapN(fa, fb, (a, b) -> mapper.curried().apply(a).apply(b)));
  }

  default <A, B, C> Map3<F, A, B, C> mapN(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc) {
    return new Map3<>(this, fa, fb, fc);
  }

  default <A, B, C, D, R> Kind<F, R> mapN(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc,
      Kind<F, ? extends D> fd,
      Function4<? super A, ? super B, ? super C, ? super D, ? extends R> mapper) {
    return ap(fd, mapN(fa, fb, fc, (a, b, c) -> mapper.curried().apply(a).apply(b).apply(c)));
  }

  default <A, B, C, D> Map4<F, A, B, C, D> mapN(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc,
      Kind<F, ? extends D> fd) {
    return new Map4<>(this, fa, fb, fc, fd);
  }

  default <A, B, C, D, E, R> Kind<F, R> mapN(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc,
      Kind<F, ? extends D> fd,
      Kind<F, ? extends E> fe,
      Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> mapper) {
    return ap(fe, mapN(fa, fb, fc, fd, (a, b, c, d) -> mapper.curried().apply(a).apply(b).apply(c).apply(d)));
  }

  default <A, B, C, D, E> Map5<F, A, B, C, D, E> mapN(
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc,
      Kind<F, ? extends D> fd,
      Kind<F, ? extends E> fe) {
    return new Map5<>(this, fa, fb, fc, fd, fe);
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

  record Map2<F extends Witness, A, B>(Applicative<F> instance, Kind<F, ? extends A> fa, Kind<F, ? extends B> fb) {

    public <R> Kind<F, R> apply(Function2<? super A, ? super B, ? extends R> apply) {
      return instance.mapN(fa, fb, apply);
    }
  }

  record Map3<F extends Witness, A, B, C>(
      Applicative<F> instance,
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc) {

    public <R> Kind<F, R> apply(Function3<? super A, ? super B, ? super C, ? extends R> apply) {
      return instance.mapN(fa, fb, fc, apply);
    }
  }

  record Map4<F extends Witness, A, B, C, D>(
      Applicative<F> instance,
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc,
      Kind<F, ? extends D> fd) {

    public <R> Kind<F, R> apply(Function4<? super A, ? super B, ? super C, ? super D, ? extends R> apply) {
      return instance.mapN(fa, fb, fc, fd, apply);
    }
  }

  record Map5<F extends Witness, A, B, C, D, E>(
      Applicative<F> instance,
      Kind<F, ? extends A> fa,
      Kind<F, ? extends B> fb,
      Kind<F, ? extends C> fc,
      Kind<F, ? extends D> fd,
      Kind<F, ? extends E> fe) {

    public <R> Kind<F, R> apply(Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> apply) {
      return instance.mapN(fa, fb, fc, fd, fe, apply);
    }
  }
}
