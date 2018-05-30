/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Sequence<E> extends Iterable<E> {

  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  <R> Sequence<R> map(Handler1<E, R> mapper);

  <R> Sequence<R> flatMap(Handler1<E, Sequence<R>> mapper);

  Sequence<E> filter(Matcher<E> matcher);

}
