/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Sequence<E> extends Iterable<E> {

  int size();

  <R> Sequence<R> map(Handler1<E, R> mapper);

  <R> Sequence<R> flatMap(SequenceHandler<E, R> mapper);

  Sequence<E> filter(Matcher<E> matcher);

  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default boolean isEmpty() {
    return size() == 0;
  }
}
