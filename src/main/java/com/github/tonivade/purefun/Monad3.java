/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Monad3<W extends Kind, T, V, U> extends Higher3<W, T, V, U>, Functor<Higher1<Higher1<W, T>, V>, U> {

  <R> Monad3<W, T, V, R> flatMap(Function1<U, ? extends Higher3<W, T, V, R>> map);

}
