/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface FlatMap3<F extends Kind, A, B, C> extends Higher3<F, A, B, C>, Mappable<Higher1<Higher1<F, A>, B>, C> {

  <R> FlatMap3<F, A, B, R> flatMap(Function1<C, ? extends Higher3<F, A, B, R>> map);

}
