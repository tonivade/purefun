/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Function2.first;
import static com.github.tonivade.purefun.core.Function2.second;

import com.github.tonivade.purefun.Kind;

public interface Applicable<F extends Applicable<F, ?>, A> extends Mappable<F, A> {

  @Override
  <R> Applicable<F, R> map(Function1<? super A, ? extends R> mapper);

  <R> Applicable<F, R> ap(Kind<F, ? extends Function1<? super A, ? extends R>> apply);

  default <B> Applicable<F, Tuple2<A, B>> zip(Kind<F, ? extends B> other) {
    return mapN(narrowK(this), narrowK(other)).apply(Tuple::of);
  }

  default <B> Applicable<F, A> zipLeft(Kind<F, ? extends B> other) {
    return mapN(narrowK(this), narrowK(other)).apply(first());
  }

  default <B> Applicable<F, B> zipRight(Kind<F, ? extends B> other) {
    return mapN(narrowK(this), narrowK(other)).apply(second());
  }

  default <B, R> Applicable<F, R> zipWith(Kind<F, ? extends B> other, Function2<? super A, ? super B, ? extends R> mapper) {
    return mapN(narrowK(this), narrowK(other)).apply(mapper);
  }

  static <F extends Applicable<F, ?>, A, B, R> Applicable<F, R> mapN(Applicable<F, ? extends A> fa, Applicable<F, ? extends B> fb,
      Function2<? super A, ? super B, ? extends R> mapper) {
    return fb.ap(fa.map(mapper.curried()));
  }

  static <F extends Applicable<F, ?>, A, B> Map2<F, A, B> mapN(Applicable<F, ? extends A> fa, Applicable<F, ? extends B> fb) {
    return new Map2<>(fa, fb);
  }

  static <F extends Applicable<F, ?>, A, B, C, R> Applicable<F, R> mapN(
      Applicable<F, ? extends A> fa,
      Applicable<F, ? extends B> fb,
      Applicable<F, ? extends C> fc,
      Function3<? super A, ? super B, ? super C, ? extends R> mapper) {
    return fc.ap(mapN(fa, fb, (a, b) -> mapper.curried().apply(a).apply(b)));
  }

  static <F extends Applicable<F, ?>, A, B, C> Map3<F, A, B, C> mapN(
      Applicable<F, ? extends A> fa,
      Applicable<F, ? extends B> fb,
      Applicable<F, ? extends C> fc) {
    return new Map3<>(fa, fb, fc);
  }

  static <F extends Applicable<F, ?>, A, B, C, D, R> Applicable<F, R> mapN(
      Applicable<F, ? extends A> fa,
      Applicable<F, ? extends B> fb,
      Applicable<F, ? extends C> fc,
      Applicable<F, ? extends D> fd,
      Function4<? super A, ? super B, ? super C, ? super D, ? extends R> mapper) {
    return fd.ap(mapN(fa, fb, fc, (a, b, c) -> mapper.curried().apply(a).apply(b).apply(c)));
  }

  static <F extends Applicable<F, ?>, A, B, C, D> Map4<F, A, B, C, D> mapN(
      Applicable<F, ? extends A> fa,
      Applicable<F, ? extends B> fb,
      Applicable<F, ? extends C> fc,
      Applicable<F, ? extends D> fd) {
    return new Map4<>(fa, fb, fc, fd);
  }

  static <F extends Applicable<F, ?>, A, B, C, D, E, R> Applicable<F, R> mapN(
      Applicable<F, ? extends A> fa,
      Applicable<F, ? extends B> fb,
      Applicable<F, ? extends C> fc,
      Applicable<F, ? extends D> fd,
      Applicable<F, ? extends E> fe,
      Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> mapper) {
    return fe.ap(mapN(fa, fb, fc, fd, (a, b, c, d) -> mapper.curried().apply(a).apply(b).apply(c).apply(d)));
  }

  static <F extends Applicable<F, ?>, A, B, C, D, E> Map5<F, A, B, C, D, E> mapN(
      Applicable<F, ? extends A> fa,
      Applicable<F, ? extends B> fb,
      Applicable<F, ? extends C> fc,
      Applicable<F, ? extends D> fd,
      Applicable<F, ? extends E> fe) {
    return new Map5<>(fa, fb, fc, fd, fe);
  }

  @SuppressWarnings("unchecked")
  static <F extends Applicable<F, ?>, A> Applicable<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Applicable<F, A>) kind;
  }

  record Map2<F extends Applicable<F, ?>, A, B>(Applicable<F, ? extends A> fa, Applicable<F, ? extends B> fb) {

    public <R> Applicable<F, R> apply(Function2<? super A, ? super B, ? extends R> apply) {
      return mapN(fa, fb, apply);
    }
  }

  record Map3<F extends Applicable<F, ?>, A, B, C>(
      Applicable<F, ? extends A> fa,
      Applicable<F, ? extends B> fb,
      Applicable<F, ? extends C> fc) {

    public <R> Applicable<F, R> apply(Function3<? super A, ? super B, ? super C, ? extends R> apply) {
      return mapN(fa, fb, fc, apply);
    }
  }

  record Map4<F extends Applicable<F, ?>, A, B, C, D>(
      Applicable<F, ? extends A> fa,
      Applicable<F, ? extends B> fb,
      Applicable<F, ? extends C> fc,
      Applicable<F, ? extends D> fd) {

    public <R> Applicable<F, R> apply(Function4<? super A, ? super B, ? super C, ? super D, ? extends R> apply) {
      return mapN(fa, fb, fc, fd, apply);
    }
  }

  record Map5<F extends Applicable<F, ?>, A, B, C, D, E>(
      Applicable<F, ? extends A> fa,
      Applicable<F, ? extends B> fb,
      Applicable<F, ? extends C> fc,
      Applicable<F, ? extends D> fd,
      Applicable<F, ? extends E> fe) {

    public <R> Applicable<F, R> apply(Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> apply) {
      return mapN(fa, fb, fc, fd, fe, apply);
    }
  }
}
