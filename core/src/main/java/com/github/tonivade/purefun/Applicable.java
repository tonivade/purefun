/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Applicable<F extends Witness, A> extends Mappable<F, A> {

  @Override
  <R> Applicable<F, R> map(Function1<? super A, ? extends R> mapper);

  <R> Applicable<F, R> ap(Kind<F, Function1<? super A, ? extends R>> apply);

  static <F extends Witness, A, B, C> Applicable<F, C> mapN(Applicable<F, ? extends A> fa, Applicable<F, ? extends B> fb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return fb.ap(fa.map(mapper.curried()));
  }

  static <F extends Witness, A, B, C, D> Applicable<F, D> mapN(
      Applicable<F, ? extends A> fa, 
      Applicable<F, ? extends B> fb, 
      Applicable<F, ? extends C> fc, 
      Function3<? super A, ? super B, ? super C, ? extends D> mapper) {
    return fc.ap(mapN(fa, fb, (a, b) -> mapper.curried().apply(a).apply(b)));
  }

  static <F extends Witness, A, B, C, D, E> Applicable<F, E> mapN(
      Applicable<F, ? extends A> fa, 
      Applicable<F, ? extends B> fb, 
      Applicable<F, ? extends C> fc, 
      Applicable<F, ? extends D> fd, 
      Function4<? super A, ? super B, ? super C, ? super D, ? extends E> mapper) {
    return fd.ap(mapN(fa, fb, fc, (a, b, c) -> mapper.curried().apply(a).apply(b).apply(c)));
  }

  static <F extends Witness, A, B, C, D, E, R> Applicable<F, R> mapN(
      Applicable<F, ? extends A> fa, 
      Applicable<F, ? extends B> fb, 
      Applicable<F, ? extends C> fc, 
      Applicable<F, ? extends D> fd, 
      Applicable<F, ? extends E> fe, 
      Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> mapper) {
    return fe.ap(mapN(fa, fb, fc, fd, (a, b, c, d) -> mapper.curried().apply(a).apply(b).apply(c).apply(d)));
  }
  
  @SuppressWarnings("unchecked")
  static <F extends Witness, A> Applicable<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Applicable<F, A>) kind;
  }
}
