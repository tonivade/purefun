/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

@FunctionalInterface
public interface Semigroup<T> {

  T combine(T t1, T t2);
}
