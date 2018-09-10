/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.Sequence.narrowK;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Matcher;
import com.github.tonivade.purefun.type.Equal;

public interface ImmutableArray<E> extends Sequence<E> {

  List<E> toList();

  @Override
  ImmutableArray<E> append(E element);
  @Override
  ImmutableArray<E> remove(E element);
  @Override
  ImmutableArray<E> appendAll(Sequence<E> other);

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
  default <R> ImmutableArray<R> flatMap(Function1<E, ? extends Higher<Sequence.µ, R>> mapper) {
    return ImmutableArray.from(stream().flatMap(element -> narrowK(mapper.apply(element)).stream()));
  }

  @Override
  default ImmutableArray<E> filter(Matcher<E> matcher) {
    return ImmutableArray.from(stream().filter(matcher::match));
  }

  static <T> ImmutableArray<T> from(Collection<T> collection) {
    return new JavaBasedImmutableArray<>(new ArrayList<>(collection));
  }

  static <T> ImmutableArray<T> from(Stream<T> stream) {
    return new JavaBasedImmutableArray<>(stream.collect(Collectors.toList()));
  }

  @SafeVarargs
  static <T> ImmutableArray<T> of(T... elements) {
    return new JavaBasedImmutableArray<>(Arrays.asList(elements));
  }

  static <T> ImmutableArray<T> empty() {
    return new JavaBasedImmutableArray<>(Collections.emptyList());
  }

  final class JavaBasedImmutableArray<E> implements ImmutableArray<E> {

    private final List<E> backend;

    private JavaBasedImmutableArray(List<E> backend) {
      this.backend = requireNonNull(backend);
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
    public Sequence<E> reverse() {
      List<E> list = toList();
      Collections.reverse(list);
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
      for (E element : other) {
        list.add(element);
      }
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> sort(Comparator<E> comparator) {
      List<E> list = toList();
      Collections.sort(list, comparator);
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
      list.addAll(position, elements.stream().collect(Collectors.toList()));
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
      return Equal.of(this)
          .append((a, b) -> Objects.equals(a.backend, b.backend))
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "ImmutableArray(" + backend + ")";
    }
  }
}
