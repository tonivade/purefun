/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface InmutableList<E> extends Sequence<E> {
  
  List<E> toList();
  
  InmutableList<E> append(E element);

  InmutableList<E> appendAll(InmutableList<E> other);

  default Option<E> head() {
    return Option.from(stream().findFirst());
  }
  
  default InmutableList<E> tail() {
    return drop(1);
  }
  
  default InmutableList<E> drop(int n) {
    return InmutableList.from(stream().skip(n));
  }

  @Override
  default <R> InmutableList<R> map(Handler1<E, R> mapper) {
    return InmutableList.from(stream().map(mapper::handle));
  }

  @Override
  default <R> InmutableList<R> flatMap(SequenceHandler<E, R> mapper) {
    return InmutableList.from(stream().flatMap(mapper.toStreamHandler()::handle));
  }

  @Override
  default InmutableList<E> filter(Matcher<E> matcher) {
    return InmutableList.from(stream().filter(matcher::match));
  }
  
  static <T> InmutableList<T> from(Collection<T> collection) {
    return new JavaBasedInmutableList<>(new ArrayList<>(collection));
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

  final class JavaBasedInmutableList<E> implements InmutableList<E> {
    private final List<E> backend;
    
    private JavaBasedInmutableList(List<E> backend) {
      this.backend = requireNonNull(backend);
    }
    
    @Override
    public int size() {
      return backend.size();
    }
    
    @Override
    public InmutableList<E> append(E element) {
      List<E> newList = new ArrayList<>(backend);
      newList.add(element);
      return new JavaBasedInmutableList<>(newList);
    }
    
    @Override
    public InmutableList<E> appendAll(InmutableList<E> other) {
      List<E> newList = new ArrayList<>(backend);
      newList.addAll(other.toList());
      return new JavaBasedInmutableList<>(newList);
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
      return equal(this)
          .append((a, b) -> Objects.equals(a.backend, b.backend))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "InmutableList(" + backend + ")";
    }
  }
}
