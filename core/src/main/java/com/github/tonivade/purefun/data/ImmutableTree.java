/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pcollections.PSortedSet;
import org.pcollections.TreePSet;

/**
 * Similar to a TreeSet
 * @param <E> the type of elements in this tree
 */
public interface ImmutableTree<E> extends Sequence<E> {

  Comparator<E> comparator();

  NavigableSet<E> toNavigableSet();

  default SequencedSet<E> toSequencedSet() {
    return toNavigableSet();
  }

  default SortedSet<E> toSortedSet() {
    return toNavigableSet();
  }

  @Override
  ImmutableTree<E> append(E element);
  @Override
  ImmutableTree<E> remove(E element);
  @Override
  ImmutableTree<E> appendAll(Sequence<? extends E> other);
  @Override
  ImmutableTree<E> removeAll(Sequence<? extends E> other);

  @Override
  ImmutableTree<E> reverse();

  Option<E> head();
  Option<E> tail();
  ImmutableTree<E> headTree(E value);
  ImmutableTree<E> tailTree(E value);
  Option<E> higher(E value);
  Option<E> lower(E value);
  Option<E> ceiling(E value);
  Option<E> floor(E value);

  @Override
  default <R> ImmutableTree<R> transduce(Transducer<? extends Sequence<R>, E, R> transducer) {
    return transduce(naturalOrder(), transducer);
  }

  <R> ImmutableTree<R> transduce(Comparator<? super R> comparator, Transducer<? extends Sequence<R>, E, R> transducer);

  @Override
  default <R> ImmutableTree<R> map(Function1<? super E, ? extends R> mapper) {
    return transduce(Transducer.map(mapper));
  }

  default <R> ImmutableTree<R> map(Comparator<? super R> comparator, Function1<? super E, ? extends R> mapper) {
    return transduce(comparator, Transducer.map(mapper));
  }

  @Override
  default <R> ImmutableTree<R> flatMap(Function1<? super E, ? extends Kind<Sequence<?>, ? extends R>> mapper) {
    return flatMap(naturalOrder(), mapper);
  }

  default <R> ImmutableTree<R> flatMap(Comparator<? super R> comparator, Function1<? super E, ? extends Kind<Sequence<?>, ? extends R>> mapper) {
    return transduce(comparator, Transducer.flatMap(mapper.andThen(SequenceOf::toSequence)));
  }

  @Override
  default ImmutableTree<E> filter(Matcher1<? super E> matcher) {
    return transduce(Transducer.filter(matcher));
  }

  @Override
  default ImmutableTree<E> filterNot(Matcher1<? super E> matcher) {
    return filter(matcher.negate());
  }

  static <T> ImmutableTree<T> from(Iterable<? extends T> iterable) {
    return from(naturalOrder(), Sequence.asStream(iterable.iterator()));
  }

  static <T> ImmutableTree<T> from(Comparator<? super T> comparator, Iterable<? extends T> iterable) {
    return from(comparator, Sequence.asStream(iterable.iterator()));
  }

  static <T> ImmutableTree<T> from(Stream<? extends T> stream) {
    return new PImmutableTree<>(naturalOrder(), stream.collect(Collectors.toCollection(TreeSet::new)));
  }

  static <T> ImmutableTree<T> from(Comparator<? super T> comparator, Stream<? extends T> stream) {
    return new PImmutableTree<>(comparator, stream.collect(Collectors.toCollection(TreeSet::new)));
  }

  @SafeVarargs
  static <T extends Comparable<? super T>> ImmutableTree<T> of(T... elements) {
    return new PImmutableTree<>(naturalOrder(), Arrays.asList(elements));
  }

  @SuppressWarnings("unchecked")
  static <T> ImmutableTree<T> empty() {
    return (ImmutableTree<T>) PImmutableTree.EMPTY;
  }

  static <T> ImmutableTree<T> empty(Comparator<? super T> comparator) {
    return new PImmutableTree<>(TreePSet.empty(comparator));
  }

  static <E> Collector<E, ?, ImmutableTree<E>> toImmutableTree() {
    return collectingAndThen(Collectors.toCollection(TreeSet::new), PImmutableTree::new);
  }

  @SuppressWarnings("unchecked")
  private static <R> Comparator<R> naturalOrder() {
    return (Comparator<R>) Comparator.naturalOrder();
  }

  final class PImmutableTree<E> implements ImmutableTree<E>, Serializable {

    @Serial
    private static final long serialVersionUID = 3964148260438348347L;

    private static final ImmutableTree<?> EMPTY = new PImmutableTree<>(TreePSet.empty(naturalOrder()));

    private static final Equal<PImmutableTree<?>> EQUAL =
        Equal.<PImmutableTree<?>>of().comparing(a -> a.backend);

    private final PSortedSet<E> backend;

    private PImmutableTree(Comparator<? super E> comparator, Collection<? extends E> backend) {
      this(TreePSet.from(comparator, backend));
    }

    private PImmutableTree(SortedSet<E> backend) {
      this(TreePSet.fromSortedSet(backend));
    }

    private PImmutableTree(PSortedSet<E> backend) {
      this.backend = checkNonNull(backend);
    }

    @Override
    public <R> ImmutableTree<R> transduce(Comparator<? super R> comparator,
        Transducer<? extends Sequence<R>, E, R> transducer) {
      var result = Transducer.transduce(transducer.narrowK(), TreePSet::plus, TreePSet.empty(comparator), this);
      return new PImmutableTree<>(result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Comparator<E> comparator() {
      return (Comparator<E>) backend.comparator();
    }

    @Override
    public int size() {
      return backend.size();
    }

    @Override
    public boolean contains(Object element) {
      return backend.contains(element);
    }

    @Override
    public ImmutableTree<E> reverse() {
      return new PImmutableTree<>(backend.descendingSet());
    }

    @Override
    public ImmutableTree<E> append(E element) {
      return new PImmutableTree<>(backend.plus(element));
    }

    @Override
    public ImmutableTree<E> remove(E element) {
      return new PImmutableTree<>(backend.minus(element));
    }

    @Override
    public ImmutableTree<E> appendAll(Sequence<? extends E> other) {
      return new PImmutableTree<>(backend.plusAll(other.toCollection()));
    }

    @Override
    public ImmutableTree<E> removeAll(Sequence<? extends E> other) {
      return new PImmutableTree<>(backend.minusAll(other.toCollection()));
    }

    @Override
    public Option<E> head() {
      return Try.of(backend::first).toOption();
    }

    @Override
    public Option<E> tail() {
      return Try.of(backend::last).toOption();
    }

    @Override
    public ImmutableTree<E> headTree(E toElement) {
      return new PImmutableTree<>(backend.headSet(toElement, false));
    }

    @Override
    public ImmutableTree<E> tailTree(E fromElement) {
      return new PImmutableTree<>(backend.tailSet(fromElement, false));
    }

    @Override
    public Option<E> higher(E value) {
      return Option.of(() -> backend.higher(value));
    }

    @Override
    public Option<E> lower(E value) {
      return Option.of(() -> backend.lower(value));
    }

    @Override
    public Option<E> ceiling(E value) {
      return Option.of(() -> backend.ceiling(value));
    }

    @Override
    public Option<E> floor(E value) {
      return Option.of(() -> backend.floor(value));
    }

    @Override
    public Iterator<E> iterator() {
      return backend.iterator();
    }

    @Override
    public NavigableSet<E> toNavigableSet() {
      return new TreeSet<>(backend);
    }

    @Override
    public int hashCode() {
      return Objects.hash(backend);
    }

    @Override
    public boolean equals(Object obj) {
      return EQUAL.applyTo(this, obj);
    }

    @Override
    public String toString() {
      return "ImmutableTree(" + backend + ")";
    }

    @Serial
    private Object readResolve() {
      if (backend.isEmpty()) {
        return EMPTY;
      }
      return this;
    }
  }
}
