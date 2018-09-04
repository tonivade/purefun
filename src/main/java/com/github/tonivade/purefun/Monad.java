/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Monad<W extends Witness, T> extends Functor<W, T> {

  <R> Monad<W, R> flatMap(Function1<T, ? extends Monad<W, R>> map);

}
