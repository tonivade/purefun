/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.stream.Collectors.groupingBy;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Sequence<E> extends Iterable<E>, Functor<E>, Filterable<E>, Foldable<E> {

  int size();
  
  boolean contains(E element);
  Sequence<E> append(E element);
  Sequence<E> remove(E element);

  Sequence<E> reverse();
  
  @Override
  <R> Sequence<R> map(Handler1<E, R> mapper);

  <R> Sequence<R> flatMap(SequenceHandler<E, R> mapper);

  @Override
  Sequence<E> filter(Matcher<E> matcher);
  
  @Override
  default Option<E> reduce(Operator2<E> operator) {
    return Option.from(stream().reduce(operator::handle));
  }
  
  @Override
  default E fold(E initial, Operator2<E> operator) {
    return stream().reduce(initial, operator::handle);
  }
  
  @Override
  default <U> U foldLeft(U initial, Handler2<U, E, U> combinator) {
    U accumulator = initial;
    for (E element : this) {
      accumulator = combinator.handle(accumulator, element);
    }
    return accumulator;
  }

  default <U> U foldRight(U initial, Handler2<E, U, U> combinator) {
    return reverse().foldLeft(initial, (acc, e) -> combinator.handle(e, acc));
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
  
  @SafeVarargs
  static <E> InmutableList<E> listOf(E... elements) {
    return InmutableList.of(elements);
  }
  
  @SafeVarargs
  static <E> InmutableSet<E> setOf(E... elements) {
    return InmutableSet.of(elements);
  }
}
