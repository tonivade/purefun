/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Mappable<F extends Kind, A> extends Higher1<F, A> {

  <R> Mappable<F, R> map(Function1<A, R> map);

}
