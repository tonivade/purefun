/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.Sequence.narrowK;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Equal;

public interface ImmutableTree<E> extends Sequence<E> {

  NavigableSet<E> toNavigableSet();

  @Override
  ImmutableTree<E> append(E element);
  @Override
  ImmutableTree<E> remove(E element);
  @Override
  ImmutableTree<E> appendAll(Sequence<E> other);
  @Override
  ImmutableTree<E> removeAll(Sequence<E> other);

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
  default <R> ImmutableTree<R> map(Function1<E, R> mapper) {
    return ImmutableTree.from(stream().map(mapper::apply));
  }

  @Override
  default <R> ImmutableTree<R> flatMap(Function1<E, ? extends Higher1<Sequence.µ, R>> mapper) {
    return ImmutableTree.from(stream().flatMap(element -> narrowK(mapper.apply(element)).stream()));
  }

  @Override
  default ImmutableTree<E> filter(Matcher1<E> matcher) {
    return ImmutableTree.from(stream().filter(matcher::match));
  }

  static <T> ImmutableTree<T> from(Iterable<T> iterable) {
    return from(Sequence.asStream(iterable.iterator()));
  }

  static <T> ImmutableTree<T> from(Stream<T> stream) {
    return new JavaBasedImmutableTree<>(stream.collect(Collectors.toCollection(TreeSet::new)));
  }

  @SafeVarargs
  static <T> ImmutableTree<T> of(T... elements) {
    return new JavaBasedImmutableTree<>(new TreeSet<>(Arrays.asList(elements)));
  }

  static <T> ImmutableTree<T> empty() {
    return new JavaBasedImmutableTree<>(Collections.emptyNavigableSet());
  }

  static <E> Collector<E, ?, ImmutableTree<E>> toImmutableTree() {
    return collectingAndThen(toCollection(TreeSet::new), JavaBasedImmutableTree::new);
  }

  final class JavaBasedImmutableTree<E> implements ImmutableTree<E>, Serializable {

    private static final long serialVersionUID = -328223831102407507L;

    private final NavigableSet<E> backend;

    private JavaBasedImmutableTree(NavigableSet<E> backend) {
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
    public ImmutableTree<E> reverse() {
      return this;
    }

    @Override
    public ImmutableTree<E> append(E element) {
      NavigableSet<E> newSet = toNavigableSet();
      newSet.add(element);
      return new JavaBasedImmutableTree<>(newSet);
    }

    @Override
    public ImmutableTree<E> remove(E element) {
      NavigableSet<E> newSet = toNavigableSet();
      newSet.remove(element);
      return new JavaBasedImmutableTree<>(newSet);
    }

    @Override
    public ImmutableTree<E> appendAll(Sequence<E> other) {
      NavigableSet<E> newSet = toNavigableSet();
      for (E element : other) {
        newSet.add(element);
      }
      return new JavaBasedImmutableTree<>(newSet);
    }

    @Override
    public ImmutableTree<E> removeAll(Sequence<E> other) {
      NavigableSet<E> newSet = toNavigableSet();
      for (E element : other) {
        newSet.remove(element);
      }
      return new JavaBasedImmutableTree<>(newSet);
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
      return new JavaBasedImmutableTree<>(backend.headSet(toElement, false));
    }

    @Override
    public ImmutableTree<E> tailTree(E fromElement) {
      return new JavaBasedImmutableTree<>(backend.tailSet(fromElement, false));
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
      return Equal.of(this)
          .append((a, b) -> Objects.equals(a.backend, b.backend))
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "ImmutableTree(" + backend + ")";
    }
  }
}
