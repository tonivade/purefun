/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;

/**
 * Similar to a ArrayList
 * @param <E> the type of elements in this array
 */
public interface ImmutableArray<E> extends Sequence<E> {

  List<E> toList();

  @Override
  ImmutableArray<E> append(E element);
  @Override
  ImmutableArray<E> remove(E element);
  @Override
  ImmutableArray<E> appendAll(Sequence<? extends E> other);
  @Override
  ImmutableArray<E> removeAll(Sequence<? extends E> other);

  @Override
  ImmutableArray<E> reverse();
  ImmutableArray<E> sort(Comparator<? super E> comparator);

  E get(int position);
  ImmutableArray<E> remove(int position);
  ImmutableArray<E> replace(int position, E element);
  ImmutableArray<E> insert(int position, E element);
  ImmutableArray<E> insertAll(int position, Sequence<? extends E> elements);

  ImmutableArray<E> drop(int n);

  ImmutableArray<E> dropWhile(Matcher1<? super E> condition);
  ImmutableArray<E> takeWhile(Matcher1<? super E> condition);

  @Override
  default <R> ImmutableArray<R> map(Function1<? super E, ? extends R> mapper) {
    return ImmutableArray.from(stream().map(mapper::apply));
  }

  @Override
  default <R> ImmutableArray<R> flatMap(Function1<? super E, ? extends Kind<Sequence<?>, ? extends R>> mapper) {
    return ImmutableArray.from(stream().flatMap(mapper.andThen(SequenceOf::toSequence).andThen(Sequence::stream)::apply));
  }

  @Override
  default ImmutableArray<E> filter(Matcher1<? super E> matcher) {
    return ImmutableArray.from(stream().filter(matcher::match));
  }

  @Override
  default ImmutableArray<E> filterNot(Matcher1<? super E> matcher) {
    return filter(matcher.negate());
  }

  static <T> ImmutableArray<T> from(Iterable<? extends T> iterable) {
    return from(Sequence.asStream(iterable.iterator()));
  }

  static <T> ImmutableArray<T> from(Stream<? extends T> stream) {
    ArrayList<T> collect = stream.collect(Collectors.toCollection(ArrayList::new));
    return new PImmutableArray<>(collect);
  }

  @SafeVarargs
  static <T> ImmutableArray<T> of(T... elements) {
    return from(Arrays.stream(elements));
  }

  @SuppressWarnings("unchecked")
  static <T> ImmutableArray<T> empty() {
    return (ImmutableArray<T>) PImmutableArray.EMPTY;
  }

  static <E> Collector<E, ?, ImmutableArray<E>> toImmutableArray() {
    return collectingAndThen(Collectors.toCollection(ArrayList::new), PImmutableArray::new);
  }

  final class PImmutableArray<E> implements ImmutableArray<E>, Serializable {

    private static final ImmutableArray<?> EMPTY = new PImmutableArray<>(TreePVector.empty());

    private static final Equal<PImmutableArray<?>> EQUAL = Equal.<PImmutableArray<?>>of().comparing(a -> a.backend);

    @Serial
    private static final long serialVersionUID = -6967820945086954257L;

    private final PVector<E> backend;

    private PImmutableArray(Collection<E> collection) {
      this(TreePVector.from(collection));
    }

    private PImmutableArray(PVector<E> backend) {
      this.backend = checkNonNull(backend);
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
    public Iterator<E> iterator() {
      return backend.iterator();
    }

    @Override
    public List<E> toList() {
      return new ArrayList<>(backend);
    }

    @Override
    public ImmutableArray<E> append(E element) {
      return new PImmutableArray<>(backend.plus(element));
    }

    @Override
    public ImmutableArray<E> remove(E element) {
      return new PImmutableArray<>(backend.minus(element));
    }

    @Override
    public ImmutableArray<E> appendAll(Sequence<? extends E> other) {
      return new PImmutableArray<>(backend.plusAll(other.toCollection()));
    }

    @Override
    public ImmutableArray<E> removeAll(Sequence<? extends E> other) {
      return new PImmutableArray<>(backend.minusAll(other.toCollection()));
    }

    @Override
    public ImmutableArray<E> dropWhile(Matcher1<? super E> matcher) {
      var current = backend;
      while (!current.isEmpty() && matcher.match(current.get(0))) {
        current = current.minus(0);
      }
      return from(current);
    }

    @Override
    public ImmutableArray<E> takeWhile(Matcher1<? super E> matcher) {
      var current = TreePVector.<E>empty();
      for (int i = 0; !backend.isEmpty() && matcher.match(backend.get(i)); i++) {
        current = current.plus(current.size(), backend.get(i));
      }
      return from(current);
    }

    @Override
    public ImmutableArray<E> drop(int n) {
      if (n >= backend.size()) {
        return empty();
      }
      return new PImmutableArray<>(backend.subList(n, backend.size()));
    }

    @Override
    public ImmutableArray<E> reverse() {
      return new PImmutableArray<>(backend.reversed());
    }

    @Override
    public ImmutableArray<E> sort(Comparator<? super E> comparator) {
      var copy = new ArrayList<>(backend);
      copy.sort(comparator);
      return new PImmutableArray<>(copy);
    }

    @Override
    public E get(int position) {
      return backend.get(position);
    }

    @Override
    public ImmutableArray<E> remove(int position) {
      return new PImmutableArray<>(backend.minus(position));
    }

    @Override
    public ImmutableArray<E> replace(int position, E element) {
      return new PImmutableArray<>(backend.with(position, element));
    }

    @Override
    public ImmutableArray<E> insert(int position, E element) {
      return new PImmutableArray<>(backend.plus(position, element));
    }

    @Override
    public ImmutableArray<E> insertAll(int position, Sequence<? extends E> elements) {
      return new PImmutableArray<>(backend.plusAll(position, elements.toCollection()));
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
      return "ImmutableArray(" + backend + ")";
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
