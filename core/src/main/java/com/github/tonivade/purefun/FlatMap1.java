/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface FlatMap1<F extends Kind, A> extends Mappable<F, A> {

  <R> FlatMap1<F, R> flatMap(Function1<A, ? extends Higher1<F, R>> map);
}
