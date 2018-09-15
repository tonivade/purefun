/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface FlatMap2<W extends Kind, T, V> extends Higher2<W, T, V>, Mappeable<Higher1<W, T>, V> {

  <R> FlatMap2<W, T, R> flatMap(Function1<V, ? extends Higher2<W, T, R>> map);

}
