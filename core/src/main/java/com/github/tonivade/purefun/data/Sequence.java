/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Operator2;
import com.github.tonivade.purefun.core.PartialFunction1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.type.Option;

@HigherKind
public non-sealed interface Sequence<E> extends SequenceOf<E>, Iterable<E>, Bindable<Sequence<?>, E> {

  int size();

  boolean contains(Object element);

  default boolean containsAll(Iterable<?> elements) {
    for (var e : elements) {
      if (!contains(e)) {
        return false;
      }
    }
    return true;
  }

  Sequence<E> append(E element);
  Sequence<E> remove(E element);
  Sequence<E> appendAll(Sequence<? extends E> other);
  Sequence<E> removeAll(Sequence<? extends E> other);

  Sequence<E> reverse();

  @Override
  <R> Sequence<R> map(Function1<? super E, ? extends R> mapper);

  @Override
  <R> Sequence<R> flatMap(Function1<? super E, ? extends Kind<Sequence<?>, ? extends R>> mapper);

  Sequence<E> filter(Matcher1<? super E> matcher);

  Sequence<E> filterNot(Matcher1<? super E> matcher);

  <R> Sequence<R> transduce(Transducer<? extends Sequence<R>, E, R> transducer);

  default Option<E> findFirst(Matcher1<? super E> matcher) {
    return Option.from(stream().filter(matcher).findFirst());
  }

  default Collection<E> toCollection() {
    return toSequencedCollection();
  }

  default SequencedCollection<E> toSequencedCollection() {
    return new SequenceCollection<>(this);
  }

  default Option<E> reduce(Operator2<E> operator) {
    return Option.from(stream().reduce(operator));
  }

  default E fold(E initial, Operator2<E> operator) {
    return stream().reduce(initial, operator);
  }

  default <U> U foldLeft(U initial, Function2<? super U, ? super E, ? extends U> combinator) {
    U accumulator = initial;
    for (E element : this) {
      accumulator = combinator.apply(accumulator, element);
    }
    return accumulator;
  }

  default <U> U foldRight(U initial, Function2<? super E, ? super U, ? extends U> combinator) {
    return reverse().foldLeft(initial, (acc, e) -> combinator.apply(e, acc));
  }

  default String join() {
    return join("");
  }

  default String join(String separator) {
    return stream().map(Object::toString).collect(joining(separator));
  }

  default String join(String separator, String prefix, String suffix) {
    return stream().map(Object::toString).collect(joining(separator, prefix, suffix));
  }

  default <R> Sequence<R> collect(PartialFunction1<? super E, ? extends R> function) {
    return transduce(Transducer.compose(Transducer.filter(function::isDefinedAt), Transducer.map(function::apply)));
  }

  @SuppressWarnings("unchecked")
  default <G> ImmutableMap<G, ImmutableList<E>> groupBy(Function1<? super E, ? extends G> selector) {
    return (ImmutableMap<G, ImmutableList<E>>)
        ImmutableMap.from(stream().collect(groupingBy(selector))).mapValues(ImmutableList::from);
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

  default ImmutableTree<E> asTree(Comparator<? super E> comparator) {
    return ImmutableTree.from(comparator, stream());
  }

  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default boolean isEmpty() {
    return size() == 0;
  }

  default E[] toArray(Function1<Integer, E[]> supplier) {
    E[] array = supplier.apply(size());
    int i = 0;
    for (E element: this) {
      array[i++] = element;
    }
    return array;
  }

  static <E> ImmutableArray<E> emptyArray() {
    return ImmutableArray.empty();
  }

  static <E> ImmutableList<E> emptyList() {
    return ImmutableList.empty();
  }

  static <E> ImmutableSet<E> emptySet() {
    return ImmutableSet.empty();
  }

  static <E> ImmutableTree<E> emptyTree() {
    return ImmutableTree.empty();
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

  static <A> Stream<A> interleave(Iterator<A> first, Iterator<A> second) {
    return zip(first, second)
        .flatMap(tuple -> Stream.of(tuple.get1(), tuple.get2()))
        .filter(Objects::nonNull);
  }

  static <A> Stream<A> interleave(Stream<A> first, Stream<A> second) {
    return interleave(first.iterator(), second.iterator());
  }

  static <A> Stream<A> interleave(Sequence<A> first, Sequence<A> second) {
    return interleave(first.stream(), second.stream());
  }

  static <E> Stream<E> asStream(Iterator<E> iterator) {
    return StreamSupport.stream(spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
  }
}

final class PairIterator<A, B> implements Iterator<Tuple2<A, B>> {

  private final Iterator<A> first;
  private final Iterator<B> second;

  PairIterator(Iterator<A> first, Iterator<B> second) {
    this.first = checkNonNull(first);
    this.second = checkNonNull(second);
  }

  @Override
  public boolean hasNext() {
    return first.hasNext() && second.hasNext();
  }

  @Override
  public Tuple2<A, B> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return Tuple.of(nextItem(first), nextItem(second));
  }

  private static <Z> Z nextItem(Iterator<Z> it) {
    return it.next();
  }
}

final class SequenceCollection<E> implements SequencedCollection<E> {

  private final Sequence<E> sequence;

  SequenceCollection(Sequence<E> sequence) {
    this.sequence = checkNonNull(sequence);
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
    return sequence.contains(o);
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

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] array) {
    if (array.length < sequence.size()) {
      return (T[]) Arrays.copyOf(toArray(), sequence.size(), array.getClass());
    }
    System.arraycopy(toArray(), 0, array, 0, sequence.size());
    if (array.length > sequence.size()) {
      array[sequence.size()] = null;
    }
    return array;
  }

  @Override
  public SequencedCollection<E> reversed() {
    return new SequenceCollection<>(sequence.reverse());
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return sequence.containsAll(c);
  }

  @Override
  public boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addFirst(E e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addLast(E e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public E removeFirst() {
    throw new UnsupportedOperationException();
  }

  @Override
  public E removeLast() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeIf(Predicate<? super E> filter) {
    throw new UnsupportedOperationException();
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