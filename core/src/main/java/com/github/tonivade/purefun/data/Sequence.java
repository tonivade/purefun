/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static java.util.Objects.requireNonNull;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;

import java.util.stream.StreamSupport;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Option;

@HigherKind
public interface Sequence<E> extends Iterable<E> {

  int size();

  boolean contains(E element);

  default boolean containsAll(Sequence<E> elements) {
    for (E e : elements) {
      if (!contains(e)) return false;
    }
    return true;
  }

  Sequence<E> append(E element);
  Sequence<E> remove(E element);
  Sequence<E> appendAll(Sequence<E> other);
  Sequence<E> removeAll(Sequence<E> other);

  Sequence<E> reverse();

  <R> Sequence<R> map(Function1<E, R> mapper);

  <R> Sequence<R> flatMap(Function1<E, Sequence<R>> mapper);

  Sequence<E> filter(Matcher1<E> matcher);

  Sequence<E> filterNot(Matcher1<E> matcher);

  default Option<E> reduce(Operator2<E> operator) {
    return Option.from(stream().reduce(operator::apply));
  }

  default E fold(E initial, Operator2<E> operator) {
    return stream().reduce(initial, operator::apply);
  }

  default <U> U foldLeft(U initial, Function2<U, E, U> combinator) {
    U accumulator = initial;
    for (E element : this) {
      accumulator = combinator.apply(accumulator, element);
    }
    return accumulator;
  }

  default <U> U foldRight(U initial, Function2<E, U, U> combinator) {
    return reverse().foldLeft(initial, (acc, e) -> combinator.apply(e, acc));
  }

  default String join(String separator) {
    return stream().map(Object::toString).collect(joining(separator));
  }

  default String join(String separator, String prefix, String suffix) {
    return stream().map(Object::toString).collect(joining(separator, prefix, suffix));
  }

  default <R> Sequence<R> collect(PartialFunction1<E, R> function) {
    return filter(function::isDefinedAt).map(function::apply);
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

  default ImmutableTree<E> asTree() {
    return ImmutableTree.from(stream());
  }

  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default boolean isEmpty() {
    return size() == 0;
  }

  default Stream<Tuple2<Integer, E>> zipWithIndex() {
    return isEmpty() ? Stream.empty() : zip(Range.of(0, size()).stream(), stream());
  }

  default E[] toArray(Function1<Integer, E[]> supplier) {
    E[] array = supplier.apply(size());
    int i = 0;
    for (E element: this) {
      array[i++] = element;
    }
    return array;
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
  static <E extends Comparable<E>> ImmutableTree<E> treeOf(E... elements) {
    return ImmutableTree.of(elements);
  }

  static <A, B> Stream<Tuple2<A, B>> zip(Iterator<A> first, Iterator<B> second) {
    return asStream(new PairIterator<>(first, second));
  }

  static <A, B> Stream<Tuple2<A, B>> zip(Stream<A> first, Stream<B> second) {
    return zip(first.iterator(), second.iterator());
  }

  static <A, B> Stream<Tuple2<A, B>> zip(Sequence<A> first, Sequence<B> second) {
    return zip(first.stream(), second.stream());
  }

  static <E> Stream<E> asStream(Iterator<E> iterator) {
    return StreamSupport.stream(spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
  }
}

final class PairIterator<A, B> implements Iterator<Tuple2<A, B>> {

  private final Iterator<A> first;
  private final Iterator<B> second;

  PairIterator(Iterator<A> first, Iterator<B> second) {
    this.first = requireNonNull(first);
    this.second = requireNonNull(second);
  }

  @Override
  public boolean hasNext() {
    return first.hasNext() || second.hasNext();
  }

  @Override
  public Tuple2<A, B> next() {
    return Tuple.of(_next(first), _next(second));
  }

  private static <Z> Z _next(Iterator<Z> it) {
    return it.hasNext() ? it.next() : null;
  }
}

final class SequenceCollection<E> implements Collection<E> {

  private final Sequence<E> sequence;

  SequenceCollection(Sequence<E> sequence) {
    this.sequence = requireNonNull(sequence);
  }

  @Override
  public int size() {
    return sequence.size();
  }

  @Override
  public boolean isEmpty() {
    return sequence.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return sequence.contains((E) o);
  }

  @Override
  public Iterator<E> iterator() {
    return sequence.iterator();
  }

  @Override
  public Object[] toArray() {
    Object[] array = new Object[sequence.size()];
    int i = 0;
    for (E element: sequence) {
      array[i++] = element;
    }
    return array;
  }

  @Override
  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    Sequence<?> from = ImmutableList.from(c);
    return sequence.containsAll((Sequence<E>) from);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}