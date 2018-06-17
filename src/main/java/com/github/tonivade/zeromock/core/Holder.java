/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.NoSuchElementException;

public interface Holder<T> {

  T get() throws NoSuchElementException;

  <V> Holder<V> flatten() throws UnsupportedOperationException;
}
