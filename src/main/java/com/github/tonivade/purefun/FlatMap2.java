/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface FlatMap2<F extends Kind, A, B> extends Higher2<F, A, B>, Mappable<Higher1<F, A>, B> {

  <R> FlatMap2<F, A, R> flatMap(Function1<B, ? extends Higher2<F, A, R>> map);

}
