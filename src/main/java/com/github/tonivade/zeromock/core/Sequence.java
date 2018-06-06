/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.stream.Collectors.groupingBy;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Sequence<E> extends Iterable<E>, Functor<E>, Filter<E> {

  int size();
  
  boolean contains(E element);

  @Override
  <R> Sequence<R> map(Handler1<E, R> mapper);

  <R> Sequence<R> flatMap(SequenceHandler<E, R> mapper);

  @Override
  Sequence<E> filter(Matcher<E> matcher);
  
  default Option<E> reduce(Handler2<E, E, E> operator) {
    return Option.from(stream().reduce(operator::handle));
  }
  
  default E fold(E initial, Handler2<E, E, E> operator) {
    return stream().reduce(initial, operator::handle);
  }
  
  default <U> U fold(U initial, Handler2<U, E, U> combinator, Operator2<U> operator) {
    return stream().reduce(initial, combinator::handle, operator::handle);
  }
  
  default <G> InmutableMap<G, InmutableList<E>> groupBy(Handler1<E, G> getter) {
    return InmutableMap.from(stream().collect(groupingBy(getter::handle))).mapValues(InmutableList::from);
  }

  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default boolean isEmpty() {
    return size() == 0;
  }
}
