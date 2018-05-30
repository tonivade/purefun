/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface InmutableList<E> extends Sequence<E> {
  
  List<E> toList();
  
  default int size() {
    return (int) stream().count();
  }
  
  default boolean contains(E element) {
    return stream().filter(e -> e.equals(element)).findFirst().isPresent();
  }

  default Option<E> head() {
    return Option.from(stream().findFirst());
  }
  
  default InmutableList<E> tail() {
    return InmutableList.from(stream().skip(1));
  }
  
  default InmutableList<E> add(E element) {
    return concat(InmutableList.of(element));
  }

  default InmutableList<E> concat(InmutableList<E> other) {
    return InmutableList.from(Stream.concat(stream(), other.stream()));
  }

  default <R> InmutableList<R> map(Handler1<E, R> mapper) {
    return InmutableList.from(stream().map(mapper::handle));
  }

  default <R> InmutableList<R> flatMap(Handler1<E, Sequence<R>> mapper) {
    return InmutableList.from(stream().flatMap(asStream(mapper)::handle));
  }

  default InmutableList<E> filter(Matcher<E> matcher) {
    return InmutableList.from(stream().filter(matcher::match));
  }
  
  default InmutableList<E> skip(int n) {
    return InmutableList.from(stream().skip(n));
  }

  default boolean isEmpty() {
    return size() == 0;
  }
  
  static <T> InmutableList<T> from(List<T> list) {
    return new JavaBasedInmutableList<>(list);
  }
  
  static <T> InmutableList<T> from(Stream<T> stream) {
    return new JavaBasedInmutableList<>(stream.collect(Collectors.toList()));
  }
  
  @SafeVarargs
  static <T> InmutableList<T> of(T... elements) {
    return new JavaBasedInmutableList<>(asList(elements));
  }

  static <T> InmutableList<T> empty() {
    return new JavaBasedInmutableList<>(emptyList());
  }
  
  static <T, R> StreamHandler<T, R> asStream(Handler1<T, Sequence<R>> listHandler) {
    return listHandler.andThen(Sequence::stream)::handle;
  }

  static final class JavaBasedInmutableList<E> implements InmutableList<E> {
    private final List<E> backend;
    
    private JavaBasedInmutableList(List<E> backend) {
      this.backend = requireNonNull(backend);
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
    public int hashCode() {
      return Objects.hash(backend);
    }
    
    @Override
    public boolean equals(Object obj) {
      return Equal.equal(this)
          .append((a, b) -> Objects.equals(a.backend, b.backend))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "InmutableList(" + backend + ")";
    }
  }
}
