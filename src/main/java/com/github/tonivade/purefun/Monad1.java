/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Monad1<W extends Kind, T> extends Functor<W, T> {

  <R> Monad1<W, R> flatMap(Function1<T, ? extends Higher1<W, R>> map);

}
