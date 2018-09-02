/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Monad2<W, T, V> extends Higher2<W, T, V>, Functor<W, V> {

  <R> Monad2<W, T, R> flatMap(Function1<V, ? extends Monad2<W, T, R>> map);

}
