/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.Sequence.narrowK;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Matcher;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Equal;

public interface ImmutableList<E> extends Sequence<E> {

  List<E> toList();

  @Override
  ImmutableList<E> append(E element);
  @Override
  ImmutableList<E> remove(E element);
  @Override
  ImmutableList<E> appendAll(Sequence<E> other);

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
  default <R> ImmutableList<R> flatMap(Function1<E, ? extends Higher1<Sequence.µ, R>> mapper) {
    return ImmutableList.from(stream().flatMap(element -> narrowK(mapper.apply(element)).stream()));
  }

  @Override
  default ImmutableList<E> filter(Matcher<E> matcher) {
    return ImmutableList.from(stream().filter(matcher::match));
  }

  static <T> ImmutableList<T> from(Collection<T> collection) {
    return new JavaBasedImmutableList<>(new LinkedList<>(collection));
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

  final class JavaBasedImmutableList<E> implements ImmutableList<E> {

    private final List<E> backend;

    private JavaBasedImmutableList(List<E> backend) {
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
      for (E element : other) {
        newList.add(element);
      }
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
      return Equal.of(this)
          .append((a, b) -> Objects.equals(a.backend, b.backend))
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "ImmutableList(" + backend + ")";
    }
  }
}
