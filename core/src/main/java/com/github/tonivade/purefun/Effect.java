/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.time.Duration;

public interface Effect<F extends Witness, A> extends Bindable<F, A>, Applicable<F, A> {
  
  @Override
  <R> Effect<F, R> map(Function1<? super A, ? extends R> mapper);

  @Override
  <R> Effect<F, R> ap(Kind<F, Function1<? super A, ? extends R>> apply);

  @Override
  <R> Effect<F, R> flatMap(Function1<? super A, ? extends Kind<F, ? extends R>> mapper);

  <R> Effect<F, R> andThen(Kind<F, ? extends R> next);

  Effect<F, Tuple2<Duration, A>> timed();

  default Effect<F, A> repeat() {
    return repeat(1);
  }

  Effect<F, A> repeat(int times);

  default Effect<F, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  Effect<F, A> repeat(Duration delay, int times);

  default Effect<F, A> retry() {
    return retry(1);
  }

  Effect<F, A> retry(int maxRetries);

  default Effect<F, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  Effect<F, A> retry(Duration delay, int maxRetries);

  default <B> Effect<F, Tuple2<A, B>> zip(Kind<F, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }

  default <B> Effect<F, A> zipLeft(Kind<F, ? extends B> other) {
    return zipWith(other, Function2.first());
  }

  default <B> Effect<F, B> zipRight(Kind<F, ? extends B> other) {
    return zipWith(other, Function2.second());
  }

  default <B, C> Effect<F, C> zipWith(Kind<F, ? extends B> other, Function2<? super A, ? super B, ? extends C> mapper) {
    return mapN(this, narrowK(other), mapper);
  }

  static <F extends Witness, A, B, C> Effect<F, C> mapN(Effect<F, ? extends A> fa, Effect<F, ? extends B> fb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return fb.ap(fa.map(mapper.curried()));
  }

  static <F extends Witness, A, B, C, D> Effect<F, D> mapN(
      Effect<F, ? extends A> fa, 
      Effect<F, ? extends B> fb, 
      Effect<F, ? extends C> fc, 
      Function3<? super A, ? super B, ? super C, ? extends D> mapper) {
    return fc.ap(mapN(fa, fb, (a, b) -> mapper.curried().apply(a).apply(b)));
  }

  static <F extends Witness, A, B, C, D, E> Effect<F, E> mapN(
      Effect<F, ? extends A> fa, 
      Effect<F, ? extends B> fb, 
      Effect<F, ? extends C> fc, 
      Effect<F, ? extends D> fd, 
      Function4<? super A, ? super B, ? super C, ? super D, ? extends E> mapper) {
    return fd.ap(mapN(fa, fb, fc, (a, b, c) -> mapper.curried().apply(a).apply(b).apply(c)));
  }

  static <F extends Witness, A, B, C, D, E, R> Effect<F, R> mapN(
      Effect<F, ? extends A> fa, 
      Effect<F, ? extends B> fb, 
      Effect<F, ? extends C> fc, 
      Effect<F, ? extends D> fd, 
      Effect<F, ? extends E> fe, 
      Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> mapper) {
    return fe.ap(mapN(fa, fb, fc, fd, (a, b, c, d) -> mapper.curried().apply(a).apply(b).apply(c).apply(d)));
  }
  
  @SuppressWarnings("unchecked")
  static <F extends Witness, A> Effect<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Effect<F, A>) kind;
  }
}
