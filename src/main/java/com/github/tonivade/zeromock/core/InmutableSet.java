/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface InmutableSet<E> extends Sequence<E> {
  
  Set<E> toSet();

  boolean contains(E element);
  
  InmutableSet<E> add(E element);
  InmutableSet<E> remove(E element);

  InmutableSet<E> union(InmutableSet<E> other);
  InmutableSet<E> intersection(InmutableSet<E> other);
  InmutableSet<E> difference(InmutableSet<E> other);

  @Override
  default <R> InmutableSet<R> map(Handler1<E, R> mapper) {
    return InmutableSet.from(stream().map(mapper::handle));
  }

  @Override
  default <R> InmutableSet<R> flatMap(SequenceHandler<E, R> mapper) {
    return InmutableSet.from(stream().flatMap(mapper.toStreamHandler()::handle));
  }

  @Override
  default InmutableSet<E> filter(Matcher<E> matcher) {
    return InmutableSet.from(stream().filter(matcher::match));
  }
  
  static <T> InmutableSet<T> from(Collection<T> collection) {
    return new JavaBasedInmutableSet<>(new HashSet<>(collection));
  }
  
  static <T> InmutableSet<T> from(Stream<T> stream) {
    return new JavaBasedInmutableSet<>(stream.collect(Collectors.toSet()));
  }
  
  @SafeVarargs
  static <T> InmutableSet<T> of(T... elements) {
    return new JavaBasedInmutableSet<>(new HashSet<>(asList(elements)));
  }

  static <T> InmutableSet<T> empty() {
    return new JavaBasedInmutableSet<>(emptySet());
  }

  final class JavaBasedInmutableSet<E> implements InmutableSet<E> {
    private final Set<E> backend;
    
    private JavaBasedInmutableSet(Set<E> backend) {
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
    public InmutableSet<E> add(E element) {
      Set<E> newSet = new HashSet<>(backend);
      newSet.add(element);
      return new JavaBasedInmutableSet<>(newSet);
    }
    
    @Override
    public InmutableSet<E> remove(E element) {
      Set<E> newSet = new HashSet<>(backend);
      newSet.remove(element);
      return new JavaBasedInmutableSet<>(newSet);
    }
    
    @Override
    public InmutableSet<E> union(InmutableSet<E> other) {
      Set<E> newSet = new HashSet<>(backend);
      newSet.addAll(other.toSet());
      return new JavaBasedInmutableSet<>(newSet);
    }
    
    @Override
    public InmutableSet<E> intersection(InmutableSet<E> other) {
      Set<E> newSet = new HashSet<>(backend);
      newSet.retainAll(other.toSet());
      return new JavaBasedInmutableSet<>(newSet);
    }
    
    @Override
    public InmutableSet<E> difference(InmutableSet<E> other) {
      Set<E> newSet = new HashSet<>(backend);
      newSet.removeAll(other.toSet());
      return new JavaBasedInmutableSet<>(newSet);
    }
    
    @Override
    public Iterator<E> iterator() {
      return backend.iterator();
    }
    
    @Override
    public Set<E> toSet() {
      return new HashSet<>(backend);
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
      return "InmutableSet(" + backend + ")";
    }
  }
}
