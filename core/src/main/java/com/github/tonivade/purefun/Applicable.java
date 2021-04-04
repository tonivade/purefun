/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Applicable<F extends Witness, A> extends Mappable<F, A> {

  @Override
  <R> Applicable<F, R> map(Function1<? super A, ? extends R> mapper);

  <R> Applicable<F, R> ap(Kind<F, Function1<? super A, ? extends R>> apply);
}
