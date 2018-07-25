/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Option;

public interface Foldable<T> {

  Option<T> reduce(Operator2<T> operator);
  
  T fold(T initial, Operator2<T> operator);
  
  <V> V foldLeft(V initial, Function2<V, T, V> combinator);

  <V> V foldRight(V initial, Function2<T, V, V> combinator);
}
