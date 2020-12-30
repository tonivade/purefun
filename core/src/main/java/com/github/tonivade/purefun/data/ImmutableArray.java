/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.collectingAndThen;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Matcher1;

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

  default ImmutableArray<E> drop(int n) {
    return ImmutableArray.from(stream().skip(n));
  }

  @Override
  default <R> ImmutableArray<R> map(Function1<? super E, ? extends R> mapper) {
    return ImmutableArray.from(stream().map(mapper::apply));
  }

  @Override
  default <R> ImmutableArray<R> flatMap(Function1<? super E, ? extends Sequence<? extends R>> mapper) {
    return ImmutableArray.from(stream().flatMap(mapper.andThen(Sequence::stream)::apply));
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
    return new JavaBasedImmutableArray<>(stream.collect(Collectors.toCollection(ArrayList::new)));
  }

  @SafeVarargs
  static <T> ImmutableArray<T> of(T... elements) {
    return from(Arrays.stream(elements));
  }

  @SuppressWarnings("unchecked")
  static <T> ImmutableArray<T> empty() {
    return (ImmutableArray<T>) JavaBasedImmutableArray.EMPTY;
  }

  static <E> Collector<E, ?, ImmutableArray<E>> toImmutableArray() {
    return collectingAndThen(Collectors.toCollection(ArrayList::new), JavaBasedImmutableArray::new);
  }

  final class JavaBasedImmutableArray<E> implements ImmutableArray<E>, Serializable {

    private static final long serialVersionUID = 5728385935547829871L;
    
    private static final ImmutableArray<?> EMPTY = new JavaBasedImmutableArray<>(new ArrayList<>());

    private static final Equal<JavaBasedImmutableArray<?>> EQUAL = 
        Equal.<JavaBasedImmutableArray<?>>of().comparing(a -> a.backend);

    private final List<E> backend;

    private JavaBasedImmutableArray(ArrayList<E> backend) {
      this.backend = unmodifiableList(backend);
    }

    @Override
    public int size() {
      return backend.size();
    }

    @Override
    public boolean contains(E element) {
      return backend.contains(element);
    }

    @Override
    public ImmutableArray<E> reverse() {
      ArrayList<E> list = copy();
      Collections.reverse(list);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> sort(Comparator<? super E> comparator) {
      ArrayList<E> list = copy();
      list.sort(comparator);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public Iterator<E> iterator() {
      return backend.iterator();
    }

    @Override
    public ImmutableArray<E> append(E element) {
      ArrayList<E> list = copy();
      list.add(element);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> remove(E element) {
      ArrayList<E> list = copy();
      list.remove(element);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> appendAll(Sequence<? extends E> other) {
      ArrayList<E> list = copy();
      list.addAll(other.toCollection());
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> removeAll(Sequence<? extends E> other) {
      ArrayList<E> list = copy();
      list.removeAll(other.toCollection());
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public E get(int position) {
      return backend.get(position);
    }

    @Override
    public ImmutableArray<E> replace(int position, E element) {
      ArrayList<E> list = copy();
      list.set(position, element);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> remove(int position) {
      ArrayList<E> list = copy();
      list.remove(position);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> insert(int position, E element) {
      ArrayList<E> list = copy();
      list.add(position, element);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> insertAll(int position, Sequence<? extends E> elements) {
      ArrayList<E> list = copy();
      list.addAll(position, elements.toCollection());
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public List<E> toList() {
      return copy();
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

    private ArrayList<E> copy() {
      return new ArrayList<>(backend);
    }
  }
}
