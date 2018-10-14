/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.handler.SequenceHandler.identity;
import static java.util.stream.Collectors.groupingBy;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Foldable;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.type.Option;

public interface Sequence<E> extends Iterable<E>, FlatMap1<Sequence.µ, E>, Filterable<E>, Foldable<E> {

  final class µ implements Kind {}

  int size();

  boolean contains(E element);
  Sequence<E> append(E element);
  Sequence<E> remove(E element);
  Sequence<E> appendAll(Sequence<E> other);

  Sequence<E> reverse();

  @Override
  <R> Sequence<R> map(Function1<E, R> mapper);

  @Override
  <R> Sequence<R> flatMap(Function1<E, ? extends Higher1<Sequence.µ, R>> mapper);

  @SuppressWarnings("unchecked")
  default <V> Sequence<V> flatten() {
    try {
      return ((Sequence<Sequence<V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }

  @Override
  Sequence<E> filter(Matcher1<E> matcher);

  @Override
  default Option<E> reduce(Operator2<E> operator) {
    return Option.from(stream().reduce(operator::apply));
  }

  @Override
  default E fold(E initial, Operator2<E> operator) {
    return stream().reduce(initial, operator::apply);
  }

  @Override
  default <U> U foldLeft(U initial, Function2<U, E, U> combinator) {
    U accumulator = initial;
    for (E element : this) {
      accumulator = combinator.apply(accumulator, element);
    }
    return accumulator;
  }

  @Override
  default <U> U foldRight(U initial, Function2<E, U, U> combinator) {
    return reverse().foldLeft(initial, (acc, e) -> combinator.apply(e, acc));
  }

  default <G> ImmutableMap<G, ImmutableList<E>> groupBy(Function1<E, G> selector) {
    return ImmutableMap.from(stream().collect(groupingBy(selector::apply))).mapValues(ImmutableList::from);
  }

  default ImmutableList<E> asList() {
    return ImmutableList.from(stream());
  }

  default ImmutableArray<E> asArray() {
    return ImmutableArray.from(stream());
  }

  default ImmutableSet<E> asSet() {
    return ImmutableSet.from(stream());
  }

  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default boolean isEmpty() {
    return size() == 0;
  }

  @SafeVarargs
  static <E> ImmutableArray<E> arrayOf(E... elements) {
    return ImmutableArray.of(elements);
  }

  @SafeVarargs
  static <E> ImmutableList<E> listOf(E... elements) {
    return ImmutableList.of(elements);
  }

  @SafeVarargs
  static <E> ImmutableSet<E> setOf(E... elements) {
    return ImmutableSet.of(elements);
  }

  @SafeVarargs
  static <E> ImmutableTree<E> treeOf(E... elements) {
    return ImmutableTree.of(elements);
  }

  static <T> Sequence<T> narrowK(Higher1<Sequence.µ, T> hkt) {
    return (Sequence<T>) hkt;
  }
}
