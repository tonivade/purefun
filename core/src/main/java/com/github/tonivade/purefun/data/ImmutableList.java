/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.collectingAndThen;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.type.Option;

public interface ImmutableList<E> extends Sequence<E> {

  List<E> toList();

  @Override
  ImmutableList<E> append(E element);
  @Override
  ImmutableList<E> remove(E element);
  @Override
  ImmutableList<E> appendAll(Sequence<E> other);
  @Override
  ImmutableList<E> removeAll(Sequence<E> other);

  @Override
  ImmutableList<E> reverse();
  ImmutableList<E> sort(Comparator<E> comparator);

  default Option<E> head() {
    return Option.from(stream().findFirst());
  }

  default ImmutableList<E> tail() {
    return drop(1);
  }

  default ImmutableList<E> drop(int n) {
    return ImmutableList.from(stream().skip(n));
  }

  @Override
  default <R> ImmutableList<R> map(Function1<E, R> mapper) {
    return ImmutableList.from(stream().map(mapper::apply));
  }

  @Override
  default <R> ImmutableList<R> flatMap(Function1<E, Sequence<R>> mapper) {
    return ImmutableList.from(stream().flatMap(mapper.andThen(Sequence::stream)::apply));
  }

  @Override
  default ImmutableList<E> filter(Matcher1<E> matcher) {
    return ImmutableList.from(stream().filter(matcher::match));
  }

  @Override
  default ImmutableList<E> filterNot(Matcher1<E> matcher) {
    return filter(matcher.negate());
  }

  static <T> ImmutableList<T> from(Iterable<T> iterable) {
    return from(Sequence.asStream(iterable.iterator()));
  }

  static <T> ImmutableList<T> from(Stream<T> stream) {
    return new JavaBasedImmutableList<>(stream.collect(Collectors.toList()));
  }

  @SafeVarargs
  static <T> ImmutableList<T> of(T... elements) {
    return new JavaBasedImmutableList<>(Arrays.asList(elements));
  }

  static <T> ImmutableList<T> empty() {
    return new JavaBasedImmutableList<>(emptyList());
  }

  static <E> Collector<E, ?, ImmutableList<E>> toImmutableList() {
    return collectingAndThen(Collectors.toList(), JavaBasedImmutableList::new);
  }

  final class JavaBasedImmutableList<E> implements ImmutableList<E>, Serializable {

    private static final long serialVersionUID = -7468103369804662814L;

    private final List<E> backend;

    private JavaBasedImmutableList(List<E> backend) {
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
    public ImmutableList<E> reverse() {
      List<E> newList = toList();
      Collections.reverse(newList);
      return new JavaBasedImmutableList<>(newList);
    }

    @Override
    public ImmutableList<E> sort(Comparator<E> comparator) {
      List<E> newList = toList();
      Collections.sort(newList, comparator);
      return new JavaBasedImmutableList<>(newList);
    }

    @Override
    public ImmutableList<E> append(E element) {
      List<E> newList = toList();
      newList.add(element);
      return new JavaBasedImmutableList<>(newList);
    }

    @Override
    public ImmutableList<E> remove(E element) {
      List<E> newList = toList();
      newList.remove(element);
      return new JavaBasedImmutableList<>(newList);
    }

    @Override
    public ImmutableList<E> appendAll(Sequence<E> other) {
      List<E> newList = toList();
      newList.addAll(new SequenceCollection<>(other));
      return new JavaBasedImmutableList<>(newList);
    }

    @Override
    public ImmutableList<E> removeAll(Sequence<E> other) {
      List<E> newList = toList();
      newList.removeAll(new SequenceCollection<>(other));
      return new JavaBasedImmutableList<>(newList);
    }

    @Override
    public Iterator<E> iterator() {
      return backend.iterator();
    }

    @Override
    public List<E> toList() {
      return new LinkedList<>(backend);
    }

    @Override
    public int hashCode() {
      return Objects.hash(backend);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(JavaBasedImmutableList.class)
          .comparing(a -> a.backend)
          .applyTo(this, obj);
    }

    @Override
    public String toString() {
      return "ImmutableList(" + backend + ")";
    }
  }
}
