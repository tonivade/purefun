/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static java.util.Collections.emptyList;
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
  ImmutableArray<E> appendAll(Sequence<E> other);
  @Override
  ImmutableArray<E> removeAll(Sequence<E> other);

  @Override
  ImmutableArray<E> reverse();
  ImmutableArray<E> sort(Comparator<E> comparator);

  E get(int position);
  ImmutableArray<E> remove(int position);
  ImmutableArray<E> replace(int position, E element);
  ImmutableArray<E> insert(int position, E element);
  ImmutableArray<E> insertAll(int position, Sequence<E> elements);

  default ImmutableArray<E> drop(int n) {
    return ImmutableArray.from(stream().skip(n));
  }

  @Override
  default <R> ImmutableArray<R> map(Function1<E, R> mapper) {
    return ImmutableArray.from(stream().map(mapper::apply));
  }

  @Override
  default <R> ImmutableArray<R> flatMap(Function1<E, Sequence<R>> mapper) {
    return ImmutableArray.from(stream().flatMap(mapper.andThen(Sequence::stream)::apply));
  }

  @Override
  default ImmutableArray<E> filter(Matcher1<E> matcher) {
    return ImmutableArray.from(stream().filter(matcher::match));
  }

  @Override
  default ImmutableArray<E> filterNot(Matcher1<E> matcher) {
    return filter(matcher.negate());
  }

  static <T> ImmutableArray<T> from(Iterable<T> iterable) {
    return from(Sequence.asStream(iterable.iterator()));
  }

  static <T> ImmutableArray<T> from(Stream<T> stream) {
    return new JavaBasedImmutableArray<>(stream.collect(Collectors.toList()));
  }

  @SafeVarargs
  static <T> ImmutableArray<T> of(T... elements) {
    return new JavaBasedImmutableArray<>(Arrays.asList(elements));
  }

  static <T> ImmutableArray<T> empty() {
    return new JavaBasedImmutableArray<>(emptyList());
  }

  static <E> Collector<E, ?, ImmutableArray<E>> toImmutableArray() {
    return collectingAndThen(Collectors.toList(), JavaBasedImmutableArray::new);
  }

  final class JavaBasedImmutableArray<E> implements ImmutableArray<E>, Serializable {

    private static final long serialVersionUID = 5728385935547829871L;

    private static final Equal<JavaBasedImmutableArray<?>> EQUAL = 
        Equal.<JavaBasedImmutableArray<?>>of().comparing(a -> a.backend);

    private final List<E> backend;

    private JavaBasedImmutableArray(List<E> backend) {
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
      List<E> list = toList();
      Collections.reverse(list);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> sort(Comparator<E> comparator) {
      List<E> list = toList();
      list.sort(comparator);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public Iterator<E> iterator() {
      return backend.iterator();
    }

    @Override
    public ImmutableArray<E> append(E element) {
      List<E> list = toList();
      list.add(element);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> remove(E element) {
      List<E> list = toList();
      list.remove(element);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> appendAll(Sequence<E> other) {
      List<E> list = toList();
      list.addAll(new SequenceCollection<>(other));
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> removeAll(Sequence<E> other) {
      List<E> list = toList();
      list.removeAll(new SequenceCollection<>(other));
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public E get(int position) {
      return backend.get(position);
    }

    @Override
    public ImmutableArray<E> replace(int position, E element) {
      List<E> list = toList();
      list.set(position, element);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> remove(int position) {
      List<E> list = toList();
      list.remove(position);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> insert(int position, E element) {
      List<E> list = toList();
      list.add(position, element);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> insertAll(int position, Sequence<E> elements) {
      List<E> list = toList();
      list.addAll(position, new SequenceCollection<>(elements));
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public List<E> toList() {
      return new ArrayList<>(backend);
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
  }
}
